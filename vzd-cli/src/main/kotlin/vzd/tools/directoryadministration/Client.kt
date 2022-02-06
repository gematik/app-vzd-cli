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
import kotlinx.serialization.json.Json
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

class VZDResponseException(response: HttpResponse, message: String) :
    ResponseException(response, message) {
    override val message: String = "VZD error: ${response.call.request.url}. " +
            "Status: ${response.status}. Text: \"$message\". Reason: \"${response.headers["RS-DIRECTORY-ADMIN-ERROR"]}\" "
}

/**
 * Directory Administration API Client
 * @see <a href="https://github.com/gematik/api-vzd/blob/master/src/openapi/DirectoryAdministration.yaml">Directory Administration Open API</a>
 */
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

    /**
     * Implements POST /DirectoryEntries (add_Directory_Entry)
     */
    suspend fun addDirectoryEntry(directoryEntry: CreateDirectoryEntry): DistinguishedName {
        logger.debug { "POST ${config.apiURL} /DirectoryEntries" }

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
        logger.debug { "DELETE ${config.apiURL} /DirectoryEntries/{uid}" }
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

    /**
     * Implements GET / (getInfo)
     */
    suspend fun getInfo(): InfoObject {
        logger.debug { "GET ${config.apiURL} /" }
        val response = http.get("/")
        if (response.status != HttpStatusCode.OK) {
            throw VZDResponseException(response, "Unable to get info: ${response.body<String>()}")
        }

        return response.body()
    }

    /**
     * Implements PUT /DirectoryEntries/{uid}/baseDirectoryEntries (modify_Directory_Entry)
     */
    suspend fun modifyDirectoryEntry(uid: String, baseDirectoryEntry: UpdateBaseDirectoryEntry) {
        logger.debug { "PUT ${config.apiURL} /DirectoryEntries/{uid}/baseDirectoryEntries" }
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
    suspend fun addDirectoryEntryCertificate() {
        logger.debug { "POST ${config.apiURL} /DirectoryEntries/{uid}/Certificates" }
        TODO()
    }

    /**
     * Implements GET /DirectoryEntries/Certificates (read_Directory_Certificates)
     */
    suspend fun readDirectoryCertificates(parameters: Map<String, String>): List<UserCertificate>? {
        logger.debug { "GET ${config.apiURL} /DirectoryEntries/Certificates" }

        val response = http.get("/DirectoryEntries/Certificates") {
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

    /**
     * Implements DELETE /DirectoryEntries/{uid}/Certificates/{certificateEntryID} (delete_Directory_Entry_Certificate)
     */
    suspend fun deleteDirectoryEntryCertificate() {
        logger.debug { "DELETE ${config.apiURL} /DirectoryEntries/{uid}/Certificates/{certificateEntryID}" }
        TODO()
    }
}


class Configuration {
    var apiURL = ""
    var loadTokens: suspend () -> BearerTokens? = { null }
}