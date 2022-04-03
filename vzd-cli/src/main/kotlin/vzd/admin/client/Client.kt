package vzd.admin.client

import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter
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
import io.ktor.utils.io.*
import io.ktor.utils.io.jvm.javaio.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import mu.KotlinLogging
import java.io.InputStreamReader
import java.io.Reader
import java.io.StringWriter
import java.math.BigDecimal

private val logger = KotlinLogging.logger {}

private val JSON = Json {
    ignoreUnknownKeys = true
    prettyPrint = true
}

class VZDResponseException(response: HttpResponse, message: String) :
    ResponseException(response, message) {

    val details: String
        get() {
            var details = "Bad response: $response"

            val reason = response.headers["RS-DIRECTORY-ADMIN-ERROR"]
            if (reason != null) {
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

            val l = logger

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
                    loadTokens {
                        BearerTokens(config.accessToken, "")
                    }
                }
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

    val accessToken get() = config.accessToken

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
        return readDirectoryEntry(parameters, "/DirectoryEntriesSync")
    }

    /**
     * Implements GET /DirectoryEntries (read_Directory_Entry)
     */
    suspend fun readDirectoryEntry(
        parameters: Map<String, String>,
        path: String = "/DirectoryEntries",
    ): List<DirectoryEntry>? {
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
            throw VZDResponseException(response, "Unable to get entries")
        }

        return response.body()
    }

    /**
     * Queries the server using `GET /DirectoryEntries` and returns a stream of DirectoryEntry objects.
     * Allows processing of large amount of entries without loading all of them in RAM.
     */
    suspend fun streamDirectoryEntries(
        parameters: Map<String, String>,
        path: String = "/DirectoryEntriesSync",
        sink: ( entry: DirectoryEntry) -> Unit,
    ) {

        // create custom client without automatic JSON parsing
        val httpClient = HttpClient(CIO) {
            engine {
                config.httpProxyURL?.let {
                    logger.debug { "Using proxy: $it" }
                    proxy = ProxyBuilder.http(it)
                }
            }

            install(HttpTimeout) {
                requestTimeoutMillis = 1000 * 60 * 60
            }
            expectSuccess = false

            install(Auth) {
                bearer {
                    sendWithoutRequest {
                        true
                    }
                    loadTokens {
                        BearerTokens(config.accessToken, "")
                    }
                }
            }

            defaultRequest {
                url(config.apiURL)
                headers.set("Accept", "application/json")
            }

        }
        httpClient.prepareGet(path) {
            for (param in parameters.entries) {
                parameter(param.key, param.value)
            }
        }
            .execute { response ->
            if (response.status != HttpStatusCode.OK) {
                throw VZDResponseException(response, "Unable to get entries")
            }
            val channel: ByteReadChannel = response.body()
            jsonArraySequence(InputStreamReader(channel.toInputStream()))
                .forEach {
                    sink(JSON.decodeFromString(it))
                }
        }

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
    suspend fun modifyDirectoryEntry(uid: String, baseDirectoryEntry: UpdateBaseDirectoryEntry): DistinguishedName {
        val response = http.put("/DirectoryEntries/${uid}/baseDirectoryEntries") {
            contentType(ContentType.Application.Json)
            setBody(baseDirectoryEntry)
        }

        if (response.status != HttpStatusCode.OK) {
            throw VZDResponseException(response, "Unable to modify entry: ${response.body<String>()}")
        }

        return response.body()
    }

    /**
     * Implements POST /DirectoryEntries/{uid}/Certificates (add_Directory_Entry_Certificate)
     */
    suspend fun addDirectoryEntryCertificate(uid: String, userCertificate: UserCertificate): DistinguishedName {
        val response = http.post("/DirectoryEntries/${uid}/Certificates") {
            contentType(ContentType.Application.Json)
            setBody(userCertificate)
        }

        if (response.status != HttpStatusCode.Created) {
            throw VZDResponseException(response, "Unable to modify entry: ${response.body<String>()}")
        }

        return response.body()
    }

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
    suspend fun deleteDirectoryEntryCertificate(uid: String, certificateEntryID: String) {
        val response = http.delete("/DirectoryEntries/$uid/Certificates/$certificateEntryID")

        if (response.status != HttpStatusCode.OK) {
            throw VZDResponseException(response, "Unable to delete entry $certificateEntryID")
        }


    }
}

class ClientConfiguration {
    var apiURL = ""
    var accessToken = ""
    var httpProxyURL: String? = null
}

fun jsonArraySequence(input: Reader): Sequence<String> = sequence {
    val reader = JsonReader(input)

    reader.beginArray()
    while (reader.hasNext()) {
        val strWriter = StringWriter()
        val writer = JsonWriter(strWriter)
        reader.beginObject()
        writer.beginObject()
        var depth = 0
        while(reader.peek() != JsonToken.END_OBJECT || depth > 0) {
            val token = reader.peek()
            when (token) {
                JsonToken.BEGIN_OBJECT -> {
                    reader.beginObject()
                    writer.beginObject()
                    depth++
                }
                JsonToken.END_OBJECT -> {
                    reader.endObject()
                    writer.endObject()
                    depth--
                }
                JsonToken.NAME ->
                    writer.name(reader.nextName())
                JsonToken.BOOLEAN ->
                    writer.value(reader.nextBoolean())
                JsonToken.STRING ->
                    writer.value(reader.nextString())
                JsonToken.NULL -> {
                    reader.nextNull()
                    writer.nullValue()
                }
                JsonToken.NUMBER ->
                    writer.value(BigDecimal(reader.nextString()))
                JsonToken.BEGIN_ARRAY -> {
                    reader.beginArray()
                    writer.beginArray()
                }
                JsonToken.END_ARRAY -> {
                    reader.endArray()
                    writer.endArray()
                }
                else -> reader.skipValue()
            }

        }

        reader.endObject()
        writer.endObject()
        yield(strWriter.toString())

    }
    reader.endArray()
}
