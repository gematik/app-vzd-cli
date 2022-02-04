package vzd.tools.directoryadministration

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.auth.*
import io.ktor.client.plugins.auth.providers.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

class Client {
    private val config: Configuration = Configuration();
    private val http: HttpClient

    constructor(block: Configuration.() -> Unit = {}) {
        block(this.config)

        this.http = HttpClient(CIO) {
            expectSuccess = false
            install(Logging) {
                logger = Logger.DEFAULT
                level = LogLevel.NONE
            }
            install(Auth) {
                bearer {
                    sendWithoutRequest {
                        true
                    }
                    loadTokens ( config.loadTokens )
                }
            }
            install(ContentNegotiation) {
                json(
                    Json {
                        ignoreUnknownKeys = true
                    }
                )
            }
            defaultRequest {
                url(config.apiURL)
            }
        }
    }

    suspend fun readDirectoryEntry(parameters: Map<String, String>): List<DirectoryEntry>? {
        logger.debug { "GET ${config.apiURL}" }

        val response = http.get("/DirectoryEntries") {
            for (param in parameters.entries) {
                parameter(param.key, param.value)
            }
        }

        if (response.status == HttpStatusCode.NotFound) {
            logger.debug { "Server returned 404 Not Found" }
            return null
        }

        return response.body();
    }
}

class Configuration {
    var apiURL = ""
    var loadTokens: suspend () -> BearerTokens? = { null }
}