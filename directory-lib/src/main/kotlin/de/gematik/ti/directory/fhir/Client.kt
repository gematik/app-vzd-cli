package de.gematik.ti.directory.fhir

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.parser.IParser
import de.gematik.ti.directory.DirectoryAuthPlugin
import de.gematik.ti.directory.DirectoryAuthPluginConfig
import de.gematik.ti.directory.DirectoryException
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import mu.KotlinLogging
import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.model.OperationOutcome
import org.hl7.fhir.r4.model.Resource
import org.hl7.fhir.r4.model.ResourceType

val FHIR_R4: FhirContext = FhirContext.forR4()

private val JSON =
    Json {
        ignoreUnknownKeys = true
        prettyPrint = true
    }

enum class Scope(
    val searchBasePath: String
) {
    Search("/search"),
    Owner("/owner"),
    Fdv("/fdv/search"),
}

enum class SearchResource(
    val resourceType: ResourceType
) {
    PractitionerRole(ResourceType.PractitionerRole),
    HealthcareService(ResourceType.HealthcareService),
}

class SearchQuery(
    val resource: SearchResource,
    val params: MutableMap<String, List<String>> = mutableMapOf()
) {
    fun addParam(
        key: String,
        value: String
    ) {
        if (params.containsKey(key)) {
            params[key] = params[key]!!.plus(value)
        } else {
            params[key] = listOf(value)
        }
    }
}

class Client(
    block: Configurator.() -> Unit = {}
) {
    private val logger = KotlinLogging.logger {}
    private val configurator: Configurator = Configurator()
    private val envConfig: EnvironmentConfig

    private val httpClient: HttpClient

    class Configurator {
        var envConfig: EnvironmentConfig? = null
        var httpProxyURL: String? = null
        internal var auth: DirectoryAuthPluginConfig.() -> Unit = {}

        fun auth(block: DirectoryAuthPluginConfig.() -> Unit) {
            auth = block
        }
    }

    fun createHttpClient(
        defaultURL: String,
        authBlock: DirectoryAuthPluginConfig.() -> Unit
    ): HttpClient =
        HttpClient(CIO) {
            engine {
                configurator.httpProxyURL?.let {
                    logger.debug { "Using proxy: $it" }
                    proxy = ProxyBuilder.http(it)
                }
            }

            expectSuccess = false

            val l = logger

            install(HttpTimeout) {
                requestTimeoutMillis = 1000 * 60 * 60
            }

            install(Logging) {
                logger = Logger.DEFAULT
                if (l.isDebugEnabled) {
                    level = LogLevel.ALL
                } else if (l.isInfoEnabled) {
                    level = LogLevel.INFO
                }
            }

            install(DirectoryAuthPlugin) {
                authBlock(this)
            }

            install(ContentNegotiation) {
                json(JSON)
            }
            defaultRequest {
                url(defaultURL)
            }
        }

    init {
        block(configurator)
        this.envConfig = configurator.envConfig ?: throw ConfigException("No environment configured")
        httpClient =
            createHttpClient(
                envConfig.search.apiURL,
                configurator.auth,
            )
    }

    suspend fun searchFdv(query: SearchQuery) = doSearch(Scope.Fdv, query)

    suspend fun search(query: SearchQuery) = doSearch(Scope.Search, query)

    suspend fun searchOwner(query: SearchQuery) = doSearch(Scope.Owner, query)

    private suspend fun doSearch(
        scope: Scope,
        query: SearchQuery,
    ): Bundle {
        logger.debug { "Searching ${scope.searchBasePath}/${query.resource.name} with query: ${query.params}" }
        val response =
            httpClient.get("${scope.searchBasePath}/${query.resource.name}") {
                query.params.forEach { (key, values) ->
                    values.forEach { value ->
                        parameter(key, value)
                    }
                }
            }
        return handleSearchResponse(response)
    }

    private suspend fun handleSearchResponse(response: HttpResponse): Bundle {
        val parser = FHIR_R4.newJsonParser()

        if (response.status != HttpStatusCode.OK) {
            throw parseError(parser, response)
        }

        val body = response.body<String>()
        val bundle = parser.parseResource(Bundle::class.java, body)
        logger.debug { "Got search response bundle with ${bundle.total} resources." }
        return bundle
    }

    private fun parseError(
        parser: IParser,
        response: HttpResponse
    ): DirectoryException {
        var exc: DirectoryException?

        val body = runBlocking { response.body<String>() }

        try {
            val outcome = parser.parseResource(OperationOutcome::class.java, body)
            exc = DirectoryException(outcome.issue.joinToString { it.diagnostics })
        } catch (_: Exception) {
            exc =
                if (response.status == HttpStatusCode.Unauthorized) {
                    DirectoryException("Unauthorized. Please use `vzd-cli login` first.")
                } else {
                    DirectoryException("Search failed: ${response.status} $body")
                }
        }
        return exc!!
    }

    suspend fun put(resource: Resource) {
        val body = FHIR_R4.newJsonParser().encodeResourceToString(resource)

        val response =
            httpClient.put("/owner/${resource.resourceType.name}/${resource.idElement.idPart}") {
                contentType(ContentType.parse("application/fhir+json"))
                setBody(body)
            }

        val parser = FHIR_R4.newJsonParser()
        if (response.status != HttpStatusCode.OK) {
            throw parseError(parser, response)
        }
    }

    fun findEntry(
        scope: Scope,
        telematikID: String,
        resource: SearchResource? = null,
    ): Bundle {
        var practitionerBundle: Bundle? = null
        var healthcareServiceBundle: Bundle? = null

        runBlocking {
            val practitionerJob =
                launch {
                    if (resource != null && resource != SearchResource.PractitionerRole) {
                        return@launch
                    }
                    val practitionerRoleQuery = SearchQuery(SearchResource.PractitionerRole)
                    practitionerRoleQuery.addParam(
                        "practitioner.identifier",
                        "https://gematik.de/fhir/sid/telematik-id|$telematikID",
                    )
                    if (scope != Scope.Owner) {
                        practitionerRoleQuery.addParam("practitioner.active", "true")
                    }
                    practitionerRoleQuery.addParam("_include", "PractitionerRole:practitioner")
                    practitionerRoleQuery.addParam("_include", "PractitionerRole:location")
                    practitionerRoleQuery.addParam("_include", "PractitionerRole:endpoint")
                    practitionerBundle = doSearch(scope, practitionerRoleQuery)
                }
            val healthcareServiceJob =
                launch {
                    if (resource != null && resource != SearchResource.HealthcareService) {
                        return@launch
                    }
                    val healthcareServiceQuery = SearchQuery(SearchResource.HealthcareService)
                    healthcareServiceQuery.addParam(
                        "organization.identifier",
                        "https://gematik.de/fhir/sid/telematik-id|$telematikID",
                    )
                    if (scope != Scope.Owner) {
                        healthcareServiceQuery.addParam("organization.active", "true")
                    }
                    healthcareServiceQuery.addParam("_include", "HealthcareService:organization")
                    healthcareServiceQuery.addParam("_include", "HealthcareService:location")
                    healthcareServiceQuery.addParam("_include", "HealthcareService:endpoint")
                    healthcareServiceBundle = doSearch(scope, healthcareServiceQuery)
                }

            practitionerJob.join()
            healthcareServiceJob.join()
        }
        val bundle =
            practitionerBundle?.takeIf { it.total > 0 }
                ?: healthcareServiceBundle?.takeIf { it.total > 0 }
                ?: throw DirectoryException("Entry with TelematikID `$telematikID` not found or cannot be edited.")
        return bundle
    }
}
