package de.gematik.ti.directory.fhir

import ca.uhn.fhir.context.FhirContext
import de.gematik.ti.directory.DirectoryAuthPlugin
import de.gematik.ti.directory.DirectoryAuthPluginConfig
import de.gematik.ti.directory.DirectoryEnvironment
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
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import mu.KotlinLogging
import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.model.OperationOutcome
import org.hl7.fhir.r4.model.ResourceType

val FHIR_R4 = FhirContext.forR4()

private val JSON =
    Json {
        ignoreUnknownKeys = true
        prettyPrint = true
    }

val DefaultConfig =
    Config(
        environments =
            mapOf(
                "tu" to
                    EnvironmentConfig(
                        search =
                            SearchConfig(
                                apiURL = "https://fhir-directory-test.vzd.ti-dienste.de/search",
                            ),
                        fdv =
                            FdvConfig(
                                apiURL = "https://fhir-directory-test.vzd.ti-dienste.de/fdv/search",
                                authenticationEndpoint = "https://auth-test.vzd.ti-dienste.de:9443/auth/realms/Service-Authenticate/protocol/openid-connect/token",
                                authorizationEndpoint = "https://fhir-directory-test.vzd.ti-dienste.de/service-authenticate",
                            ),
                    ),
                "ru" to
                    EnvironmentConfig(
                        search =
                            SearchConfig(
                                apiURL = "https://fhir-directory-ref.vzd.ti-dienste.de/search",
                            ),
                        fdv =
                            FdvConfig(
                                apiURL = "https://fhir-directory-ref.vzd.ti-dienste.de/fdv/search",
                                authenticationEndpoint = "https://auth-ref.vzd.ti-dienste.de:9443/auth/realms/Service-Authenticate/protocol/openid-connect/token",
                                authorizationEndpoint = "https://fhir-directory-ref.vzd.ti-dienste.de/service-authenticate",
                            ),
                    ),
                "pu" to
                    EnvironmentConfig(
                        search =
                            SearchConfig(
                                apiURL = "https://fhir-directory.vzd.ti-dienste.de/search",
                            ),
                        fdv =
                            FdvConfig(
                                apiURL = "https://fhir-directory.vzd.ti-dienste.de/fdv/search",
                                authenticationEndpoint = "https://auth.vzd.ti-dienste.de:9443/auth/realms/Service-Authenticate/protocol/openid-connect/token",
                                authorizationEndpoint = "https://fhir-directory.vzd.ti-dienste.de/service-authenticate",
                            ),
                    ),
            ),
    )

class ConfigException(message: String, cause: Throwable? = null) : DirectoryException(message, cause)

@Serializable
data class Config(
    val environments: Map<String, EnvironmentConfig>,
) {
    fun environment(env: DirectoryEnvironment) = environments[env.name] ?: throw ConfigException("Unknown environment: ${env.name}")
}

@Serializable
data class EnvironmentConfig(
    val search: SearchConfig,
    val fdv: FdvConfig,
)

@Serializable
data class SearchConfig(
    val apiURL: String,
)

@Serializable
data class FdvConfig(
    val apiURL: String,
    val authenticationEndpoint: String,
    val authorizationEndpoint: String,
)

enum class SearchResource(val resourceType: ResourceType) {
    PractitionerRole(ResourceType.PractitionerRole),
    HealthcareService(ResourceType.HealthcareService),
}

class SearchQuery(val resource: SearchResource, val params: MutableMap<String, List<String>> = mutableMapOf()) {
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

class Client(block: Configurator.() -> Unit = {}) {
    private val configurator: Configurator = Configurator()
    private val envConfig: EnvironmentConfig
    private val httpClientFdv: HttpClient get() {
        val httpClient by lazy { createHttpClient(envConfig.fdv.apiURL, configurator.authFdvBlock) }
        return httpClient
    }
    private val httpClientSearch: HttpClient get() {
        val httpClient by lazy { createHttpClient(envConfig.search.apiURL, configurator.authSearchBlock) }
        return httpClient
    }
    val logger = KotlinLogging.logger {}

    class Configurator {
        var envConfig: EnvironmentConfig? = null
        var httpProxyURL: String? = null
        internal var authFdvBlock: DirectoryAuthPluginConfig.() -> Unit = {}
        internal var authSearchBlock: DirectoryAuthPluginConfig.() -> Unit = {}

        fun authFdv(block: DirectoryAuthPluginConfig.() -> Unit) {
            authFdvBlock = block
        }

        fun authSearch(block: DirectoryAuthPluginConfig.() -> Unit) {
            authSearchBlock = block
        }
    }

    fun createHttpClient(
        defaultURL: String,
        authBlock: DirectoryAuthPluginConfig.() -> Unit
    ): HttpClient {
        return HttpClient(CIO) {
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
    }

    init {
        block(configurator)
        this.envConfig = configurator.envConfig ?: throw ConfigException("No environment configured")
    }

    suspend fun search(query: SearchQuery): Bundle {
        logger.debug { "Searching ${query.resource.name} with query: ${query.params}" }
        val response =
            httpClientSearch.get("/search/${query.resource.name}") {
                query.params.forEach { (key, values) ->
                    values.forEach { value ->
                        parameter(key, value)
                    }
                }
            }
        return handleSearchResponse(response)
    }

    private suspend fun handleSearchResponse(response: HttpResponse): Bundle {
        val body = response.body<String>()
        val parser = FHIR_R4.newJsonParser()

        if (response.status != HttpStatusCode.OK) {
            var exc: DirectoryException?

            try {
                val outcome = parser.parseResource(OperationOutcome::class.java, body)
                exc = DirectoryException(outcome.issue.joinToString { it.diagnostics })
            } catch (e: Exception) {
                if (response.status == HttpStatusCode.Unauthorized) {
                    exc = DirectoryException("Unauthorized. Please use `vzd-cli login` first.")
                } else {
                    exc = DirectoryException("Search failed: ${response.status} $body")
                }
            }

            throw exc!!
        }
        val bundle = parser.parseResource(Bundle::class.java, body)
        logger.debug { "Got search response bundle with ${bundle.total} resources." }
        return bundle
    }

    suspend fun searchFdv(query: SearchQuery): Bundle {
        logger.debug { "Searching ${query.resource.name} with query: ${query.params}" }
        val response =
            httpClientFdv.get("/fdv/search/${query.resource.name}") {
                query.params.forEach { (key, values) ->
                    values.forEach { value ->
                        parameter(key, value)
                    }
                }
            }
        return handleSearchResponse(response)
    }
}
