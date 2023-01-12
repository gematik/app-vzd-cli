package de.gematik.ti.directory.apo

import ca.uhn.fhir.context.FhirContext
import de.gematik.ti.directory.util.DirectoryAuthException
import de.gematik.ti.directory.util.DirectoryException
import io.ktor.http.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import mu.KotlinLogging
import org.hl7.fhir.r4.hapi.ctx.FhirR4
import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.model.Location
import java.net.ConnectException
import java.net.InetSocketAddress
import java.net.ProxySelector
import java.net.URL
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.net.http.HttpResponse.BodyHandlers

private val logger = KotlinLogging.logger {}
private val prettyJson = Json { prettyPrint = true }

class ApoClient(block: Configuration.() -> Unit = {}) {
    private val httpClient: HttpClient
    private val config: Configuration = Configuration()

    // initialize FHIR R4 class just to signify, that ShadowJar minimizer must include it into big-jar
    companion object {
        init {
            FhirR4()
        }
    }
    class Configuration {
        var apiURL = ""
        var apiKey = ""
        var httpProxyURL: String? = null
    }

    init {
        block(this.config)

        val httpClientBuilder = HttpClient.newBuilder()

        config.httpProxyURL?.let {
            logger.debug { "ApoClient is using proxy: $it" }
            val proxyURL = URL(config.httpProxyURL)
            httpClientBuilder.proxy(ProxySelector.of(InetSocketAddress(proxyURL.host, proxyURL.port)))
        }

        this.httpClient = httpClientBuilder.build()

        logger.debug { "ApoClient created ${config.apiURL}" }
    }

    fun search(queryString: String): Pair<String, Bundle> {
        val request = HttpRequest.newBuilder()
            .uri(
                URL(URL(config.apiURL), "Location?name=${queryString.encodeURLParameter()}").toURI(),
            )
            .header("X-API-KEY", config.apiKey)
            .GET()
            .build()

        logger.info { request }

        val response: HttpResponse<String>
        try {
            response = httpClient.send(request, BodyHandlers.ofString())
            logger.info { response }
        } catch (e: ConnectException) {
            if (config.httpProxyURL != null) {
                throw DirectoryAuthException("Unable to connect to proxy server: ${config.httpProxyURL}")
            } else {
                throw DirectoryAuthException("Unable to connect to API: ${config.apiURL}")
            }
        }

        if (response.statusCode() == 403) {
            throw DirectoryAuthException("Invalid API-Key for ${request.uri()}. Use `vzd-cli apo config` to configure API-Keys.")
        }

        val body = response.body()

        logger.debug { prettyJson.encodeToString(Json.decodeFromString<JsonObject>(body)) }

        val ctx = FhirContext.forR4()
        val parser = ctx.newJsonParser()
        val bundle = parser.parseResource(Bundle::class.java, body)
        return Pair(body, bundle)
    }

    fun getLocationByTelematikID(telematikID: String): Pair<String, Location> {
        val request = HttpRequest.newBuilder()
            .uri(
                URL(URL(config.apiURL), "Location?identifier=${telematikID.encodeURLParameter()}").toURI(),
            )
            .header("X-API-KEY", config.apiKey)
            .GET()
            .build()

        logger.info { request }

        val response: HttpResponse<String>
        try {
            response = httpClient.send(request, BodyHandlers.ofString())
            logger.info { response }
        } catch (e: ConnectException) {
            if (config.httpProxyURL != null) {
                throw DirectoryAuthException("Unable to connect to proxy server: ${config.httpProxyURL}")
            } else {
                throw DirectoryAuthException("Unable to connect to API: ${config.apiURL}")
            }
        }

        if (response.statusCode() == 403) {
            throw DirectoryAuthException("Invalid API-Key for ${request.uri()}. Use `vzd-cli apo config` to configure API-Keys.")
        }

        val body = response.body()

        logger.debug { prettyJson.encodeToString(Json.decodeFromString<JsonObject>(body)) }

        val ctx = FhirContext.forR4()
        val parser = ctx.newJsonParser()
        val bundle = parser.parseResource(Bundle::class.java, body)
        val location = (
            bundle.entry.firstOrNull()?.resource
                ?: throw DirectoryException("Pharmacy with TelematikID '$telematikID' not found")
            ) as Location
        return Pair(body, location)
    }
}
