package vzd.tools.directoryadministration

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.auth.*
import io.ktor.client.plugins.auth.providers.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

class VZDResponseException(response: HttpResponse, message: String) :
    ResponseException(response, message) {
    override val message: String = "VZD error: ${response.call.request.url}. " +
            "Status: ${response.status}. Text: \"$message\". Reason: \"${response.headers["RS-DIRECTORY-ADMIN-ERROR"]}\" "
}


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
                        prettyPrint = true
                    }
                )
            }
            defaultRequest {
                url(config.apiURL)
            }
        }
    }

    suspend fun addDirectoryEntry(directoryEntry: CreateDirectoryEntry): DistinguishedName {
        val response = http.post("/DirectoryEntries") {
            contentType(ContentType.Application.Json)
            setBody(directoryEntry)
        }

        if (response.status != HttpStatusCode.Created) {
            throw VZDResponseException(response, "Unable to create directory entry: ${response.body<String>()}")
        }

        return response.body()
    }

    suspend fun deleteDirectoryEntry(uid: String) {
        val response = http.delete("/DirectoryEntries/${uid}") {
        }

        if (response.status != HttpStatusCode.OK) {
            throw VZDResponseException(response, "Unable to delete directory entry: ${response.body<String>()}")
        }

        return response.body()
    }

    suspend fun readDirectoryEntryForSync(parameters: Map<String, String>): List<DirectoryEntry>? {
        return readDirectoryEntry(parameters,"/DirectoryEntriesSync")
    }

    suspend fun readDirectoryEntry(parameters: Map<String, String>, path: String = "/DirectoryEntries"): List<DirectoryEntry>? {
        logger.debug { "GET ${config.apiURL} ${path}" }

        val response = http.get(path) {
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