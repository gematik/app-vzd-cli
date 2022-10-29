package de.gematik.ti.directory.apo

import ca.uhn.fhir.context.FhirContext
import de.gematik.ti.directory.util.DirectoryException
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import mu.KotlinLogging
import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.model.Location

private val logger = KotlinLogging.logger {}

class ApoClient(block: Configuration.() -> Unit = {}) {
    private val http: HttpClient
    private val config: Configuration = Configuration()

    class Configuration {
        var apiURL = ""
        var apiKey = ""
        var httpProxyURL: String? = null
    }

    init {
        block(this.config)
        this.http = HttpClient(CIO) {
            engine {
                config.httpProxyURL?.let {
                    logger.debug { "ApoCLient is using proxy: $it" }
                    proxy = ProxyBuilder.http(it)
                }
            }

            val l = logger

            install(Logging) {
                logger = Logger.DEFAULT
                if (l.isDebugEnabled) {
                    level = LogLevel.ALL
                } else if (l.isInfoEnabled) {
                    level = LogLevel.INFO
                }
            }

            install(ContentNegotiation) {
                json(
                    Json {
                        ignoreUnknownKeys = true
                        prettyPrint = true
                    }
                )
            }

            defaultRequest {
                url(config.apiURL)
                header("X-API-KEY", config.apiKey)
            }
        }

        logger.debug { "ApoClient created ${config.apiURL}" }
    }

    suspend fun search(queryString: String): Pair<String, Bundle> {
        val response = http.get {
            url("Location")
            parameter("name", queryString)
        }
        val body = response.body<String>()
        val ctx = FhirContext.forR4()
        val parser = ctx.newJsonParser()
        val bundle = parser.parseResource(Bundle::class.java, body)
        return Pair(body, bundle)
    }

    suspend fun getLocationByTelamatikID(telematikID: String): Pair<String, Location> {
        val response = http.get {
            url("Location")
            parameter("identifier", telematikID)
        }
        val body = response.body<String>()
        val ctx = FhirContext.forR4()
        val parser = ctx.newJsonParser()
        val bundle = parser.parseResource(Bundle::class.java, body)
        val location = (
            bundle.entry.firstOrNull()?.resource
                ?: throw DirectoryException("Pharmacy with TelematikID '$telematikID' not found")
            ) as Location
// parse JSON
        return Pair(body, location)
    }
}