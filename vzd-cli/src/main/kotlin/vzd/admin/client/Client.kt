package vzd.tools.directoryadministration

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.*
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
import kotlin.math.log

private val logger = KotlinLogging.logger {}

class VZDResponseException(response: HttpResponse, message: String) :
    ResponseException(response, message) {

    val details: String
    get() {
        var details = "Bad response: $response"

        val reason = response.headers["RS-DIRECTORY-ADMIN-ERROR"]
        if ( reason != null ) {
            details += " Reason: $reason"
        }

        val body: String? = if (reason == null) runBlocking { response.body() } else null
        if (body != null && body.isNotEmpty()) {
            details += " Body: $body"
        }

        return details
    }

}

/**
 * Directory Administration API Client
 * @see <a href="https://github.com/gematik/api-vzd/blob/master/src/openapi/DirectoryAdministration.yaml">Directory Administration Open API</a>
 */
class Client(block: ClientConfiguration.() -> Unit = {}) {
    private val config: ClientConfiguration = ClientConfiguration()
    private val http: HttpClient

    init {
        block(this.config)
        this.http = HttpClient(CIO) {
            engine {
                config.httpProxyURL?.let {
                    logger.debug { "Using proxy: $it" }
                    proxy = ProxyBuilder.http(it)
                }
            }

            expectSuccess = false

            val l = logger;

            install(Logging) {
                logger = Logger.DEFAULT
                if (l.isDebugEnabled) {
                    level = LogLevel.ALL
                } else if (l.isInfoEnabled) {
                    level = LogLevel.INFO
                }
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

        logger.debug { "Client created ${config.apiURL}" }
    }

    /**
     * Implements POST /DirectoryEntries (add_Directory_Entry)
     */
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

    /**
     * Implements DELETE /DirectoryEntries/{uid} (add_Delete_Directory_Entry)
     */
    suspend fun deleteDirectoryEntry(uid: String) {
        val response = http.delete("/DirectoryEntries/${uid}") {
        }

        if (response.status != HttpStatusCode.OK) {
            throw VZDResponseException(response, "Unable to delete directory entry: ${response.body<String>()}")
        }

        return response.body()
    }

    /**
     * Implements GET /DirectoryEntriesSync (read_Directory_Entry_for_Sync)
     */
    suspend fun readDirectoryEntryForSync(parameters: Map<String, String>): List<DirectoryEntry>? {
        return readDirectoryEntry(parameters,"/DirectoryEntriesSync")
    }

    /**
     * Implements GET /DirectoryEntries (read_Directory_Entry)
     */
    suspend fun readDirectoryEntry(parameters: Map<String, String>, path: String = "/DirectoryEntries"): List<DirectoryEntry>? {
        val response = http.get(path) {
            for (param in parameters.entries) {
                parameter(param.key, param.value)
            }
        }

        if (response.status == HttpStatusCode.NotFound) {
            logger.debug { "Server returned 404 Not Found" }
            return null
        }

        if (response.status != HttpStatusCode.OK) {
            throw VZDResponseException(response, "No entries found")
        }

        return response.body()
    }

    /**
     * Implements GET / (getInfo)
     */
    suspend fun getInfo(): InfoObject {
        val response = http.get("/")
        if (response.status != HttpStatusCode.OK) {
            throw VZDResponseException(response, "Unable to get info}")
        }

        return response.body()
    }

    /**
     * Implements PUT /DirectoryEntries/{uid}/baseDirectoryEntries (modify_Directory_Entry)
     */
    suspend fun modifyDirectoryEntry(uid: String, baseDirectoryEntry: UpdateBaseDirectoryEntry) {
        val response = http.put("/DirectoryEntries/${uid}/baseDirectoryEntries") {
            contentType(ContentType.Application.Json)
            setBody(baseDirectoryEntry)
        }

        if (response.status != HttpStatusCode.OK) {
            throw VZDResponseException(response, "Unable to modify entry: ${response.body<String>()}")
        }
    }

    /**
     * Implements POST /DirectoryEntries/{uid}/Certificates (add_Directory_Entry_Certificate)
     */
    /*
    suspend fun addDirectoryEntryCertificate() {
        TODO()
    }
    */

    /**
     * Implements GET /DirectoryEntries/Certificates (read_Directory_Certificates)
     */
    suspend fun readDirectoryCertificates(parameters: Map<String, String>): List<UserCertificate>? {
        val response = http.get("/DirectoryEntries/Certificates") {
            for (param in parameters.entries) {
                parameter(param.key, param.value)
            }
        }

        if (response.status == HttpStatusCode.NotFound) {
            logger.debug { "Server returned 404 Not Found" }
            return null
        }

        return response.body()
    }

    /**
     * Implements DELETE /DirectoryEntries/{uid}/Certificates/{certificateEntryID} (delete_Directory_Entry_Certificate)
     */
    /*
    suspend fun deleteDirectoryEntryCertificate() {
        TODO()
    }
    */
}


class ClientConfiguration {
    var apiURL = ""
    var loadTokens: suspend () -> BearerTokens? = { null }
    var httpProxyURL: String? = null
}