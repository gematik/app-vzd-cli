package de.gematik.ti.directory.admin

import de.gematik.ti.directory.DirectoryAuthPlugin
import de.gematik.ti.directory.DirectoryAuthPluginConfig
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
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import mu.KotlinLogging

private val JSON =
    Json {
        ignoreUnknownKeys = true
        prettyPrint = true
    }

class AdminResponseException(response: HttpResponse, message: String) : ResponseException(response, message) {
    val details: String
        get() {
            var details = "Bad response: $response"

            val reason = response.headers["RS-DIRECTORY-ADMIN-ERROR"]
            if (reason != null) {
                details += " Reason: $reason"
            }

            val body: String? = if (reason == null) runBlocking { response.body() } else null
            if (!body.isNullOrEmpty()) {
                details += " Body: $body"
            }

            return details
        }
}

/**
 * Directory Administration API Client
 * @see <a href="https://github.com/gematik/api-vzd/blob/master/src/openapi/DirectoryAdministration.yaml">Directory Administration Open API</a>
 */
class Client(block: Configuration.() -> Unit = {}) {
    val logger = KotlinLogging.logger {}

    class Configuration {
        var apiURL = ""
        var httpProxyURL: String? = null

        internal var authConfigurator: DirectoryAuthPluginConfig.() -> Unit = {}

        fun auth(block: DirectoryAuthPluginConfig.() -> Unit) {
            authConfigurator = block
        }
    }

    private val config: Configuration = Configuration()
    private val http: HttpClient

    init {
        block(config)
        this.http =
            HttpClient(CIO) {
                engine {
                    config.httpProxyURL?.let {
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
                    config.authConfigurator(this)
                }

                install(ContentNegotiation) {
                    json(JSON)
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
        val response =
            http.post("/DirectoryEntries") {
                contentType(ContentType.Application.Json)
                setBody(directoryEntry)
            }

        if (response.status != HttpStatusCode.Created) {
            throw AdminResponseException(
                response,
                "Unable to create directory entry: ${response.status.description} ${response.status.description}",
            )
        }

        return response.body()
    }

    /**
     * Implements DELETE /DirectoryEntries/{uid} (add_Delete_Directory_Entry)
     */
    suspend fun deleteDirectoryEntry(uid: String) {
        val response = http.delete("/DirectoryEntries/$uid") {}

        if (response.status != HttpStatusCode.OK) {
            throw AdminResponseException(response, "Unable to delete directory entry")
        }

        return response.body()
    }

    /**
     * Implements GET /DirectoryEntriesSync (read_Directory_Entry_for_Sync_paging)
     */
    suspend fun readDirectoryEntryV2(
        parameters: Map<String, String>,
        cursorSize: Int = 100,
        cookie: String? = null,
    ): ReadDirectoryEntryForSyncResponse? {
        return fetchNextEntries(parameters, cursorSize, cookie)
    }

    /**
     * Implements GET /DirectoryEntriesSync (read_Directory_Entry_for_Sync)
     */
    @Deprecated("Use streamDirectoryEntriesPaging instaed")
    suspend fun readDirectoryEntryForSync(parameters: Map<String, String>): List<DirectoryEntry>? {
        return readDirectoryEntry(parameters, "/DirectoryEntriesSync")
    }

    /**
     * Implements GET /DirectoryEntries (read_Directory_Entry)
     */
    suspend fun readDirectoryEntry(
        parameters: Map<String, String>,
        path: String = "/DirectoryEntries",
    ): List<DirectoryEntry>? {
        logger.info { "readDirectoryEntry $parameters" }
        val response =
            http.get(path) {
                for (param in parameters.entries) {
                    parameter(param.key, param.value)
                }
            }

        if (response.status == HttpStatusCode.NotFound) {
            logger.debug { "Server returned 404 Not Found" }
            return null
        }

        if (response.status != HttpStatusCode.OK) {
            throw AdminResponseException(response, "Unable to get entries")
        }

        return response.body()
    }

    /**
     * Implements GET /v2/DirectoryEntriesSync (read_Directory_Entry_for_Sync_paging)
     */
    suspend fun streamDirectoryEntriesPaging(
        parameters: Map<String, String>,
        cursorSize: Int = 500,
        sink: (entry: DirectoryEntry) -> Unit,
    ) {
        var cookie: String? = null
        coroutineScope {
            do {
                logger.info { "Requesting $cursorSize entries, cookie: '$cookie'" }
                val syncResponse = fetchNextEntries(parameters, cursorSize, cookie)
                logger.info {
                    "Got ${syncResponse?.directoryEntries?.size ?: 0} entries, new cookie: '${syncResponse?.searchControlValue?.cookie ?: "none"}'"
                }
                launch {
                    syncResponse?.directoryEntries?.forEach { sink(it) }
                }
                cookie = syncResponse?.searchControlValue?.cookie
            } while (cookie != null && cookie != "")
        }
    }

    private suspend fun fetchNextEntries(
        parameters: Map<String, String>,
        cursorSize: Int,
        cookie: String?,
    ): ReadDirectoryEntryForSyncResponse? {
        val response =
            http.get("/v2/DirectoryEntriesSync") {
                expectSuccess = false
                for (param in parameters.entries) {
                    parameter(param.key, param.value)
                }
                parameter("size", cursorSize)
                if (cookie != null) {
                    parameter("cookie", cookie)
                }
            }
        if (response.status != HttpStatusCode.OK && response.status != HttpStatusCode.NotFound) {
            throw AdminResponseException(response, "Unable to get entries")
        } else if (response.status == HttpStatusCode.NotFound) {
            return null
        }

        return response.body()
    }

    /**
     * Implements GET / (getInfo)
     */
    suspend fun getInfo(): InfoObject {
        val response = http.get("/")
        if (response.status != HttpStatusCode.OK) {
            throw AdminResponseException(response, "Unable to get info}")
        }

        return response.body()
    }

    /**
     * Implements PUT /DirectoryEntries/{uid}/baseDirectoryEntries (modify_Directory_Entry)
     */
    suspend fun modifyDirectoryEntry(
        uid: String,
        baseDirectoryEntry: UpdateBaseDirectoryEntry,
    ): DistinguishedName {
        val response =
            http.put("/DirectoryEntries/$uid/baseDirectoryEntries") {
                contentType(ContentType.Application.Json)
                setBody(baseDirectoryEntry)
            }

        if (response.status != HttpStatusCode.OK) {
            throw AdminResponseException(response, "Unable to modify entry")
        }

        return response.body()
    }

    /**
     * Implements POST /DirectoryEntries/{uid}/Certificates (add_Directory_Entry_Certificate)
     */
    suspend fun addDirectoryEntryCertificate(
        uid: String,
        userCertificate: UserCertificate,
    ): DistinguishedName {
        val response =
            http.post("/DirectoryEntries/$uid/Certificates") {
                contentType(ContentType.Application.Json)
                setBody(userCertificate)
            }

        if (response.status != HttpStatusCode.Created) {
            throw AdminResponseException(response, "Unable to modify entry")
        }

        return response.body()
    }

    /**
     * Implements GET /DirectoryEntries/Certificates (read_Directory_Certificates)
     */
    suspend fun readDirectoryCertificates(parameters: Map<String, String>): List<UserCertificate>? {
        val response =
            http.get("/DirectoryEntries/Certificates") {
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
    suspend fun deleteDirectoryEntryCertificate(
        uid: String,
        certificateEntryID: String,
    ) {
        val response = http.delete("/DirectoryEntries/$uid/Certificates/$certificateEntryID")

        if (response.status != HttpStatusCode.OK) {
            throw AdminResponseException(response, "Unable to delete entry $certificateEntryID")
        }
    }

    /**
     * Implements GET /Log (readLog)
     */
    suspend fun readLog(parameters: Map<String, String>): List<LogEntry> {
        val response =
            http.get("/Log") {
                for (param in parameters.entries) {
                    parameter(param.key, param.value)
                }
            }

        if (response.status != HttpStatusCode.OK) {
            throw AdminResponseException(response, "Unable to get log $parameters")
        }

        return response.body()
    }

    /**
     * PUT /DirectoryEntries/{uid}/active
     */
    suspend fun stateSwitch(
        uid: String,
        active: Boolean,
    ) {
        val response =
            http.put("/DirectoryEntries/$uid/active") {
                contentType(ContentType.Application.Json)
                parameter("active", active)
            }

        if (response.status != HttpStatusCode.OK) {
            throw AdminResponseException(response, "Unable to switch state of the entry")
        }
    }
}
