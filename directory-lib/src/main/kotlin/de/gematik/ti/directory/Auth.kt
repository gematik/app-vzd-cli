package de.gematik.ti.directory

import de.gematik.ti.directory.admin.AdminResponseException
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.api.*
import io.ktor.client.plugins.auth.*
import io.ktor.client.plugins.auth.providers.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.forms.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

@Serializable
data class TokenResponse(
    val access_token: String,
    val token_type: String,
    @SerialName("not-before-policy")
    val not_before_policy: Int,
    val session_state: String? = null,
    val scope: String,
    val expires_in: Int,
    val refresh_expires_in: Int,
    val refresh_token: String? = null,
)

class ClientCredentialsAuthenticator(private val authURL: String, private val httpProxyUrl: String?) {
    suspend fun authenticate(
        clientId: String,
        clientSecret: String,
    ): BearerTokens {
        logger.debug { "Authenticating at: $authURL, client_id: $clientId" }
        val authClient =
            HttpClient(CIO) {
                httpProxyUrl?.let {
                    engine {
                        logger.debug { "Using proxy: $it" }
                        proxy = ProxyBuilder.http(it)
                    }
                }
                install(Logging) {
                    logger = Logger.DEFAULT
                    level = LogLevel.NONE
                }
                install(Auth) {
                    basic {
                        sendWithoutRequest {
                            true
                        }
                        credentials {
                            BasicAuthCredentials(clientId, clientSecret)
                        }
                    }
                }
                install(ContentNegotiation) {
                    json(
                        Json {
                            ignoreUnknownKeys = true
                        },
                    )
                }
            }

        var response =
            authClient.submitForm(
                url = authURL,
                formParameters =
                    Parameters.build {
                        append("grant_type", "client_credentials")
                    },
            )

        if (response.status != HttpStatusCode.OK) {
            throw AdminResponseException(response, "Authentication failed: ${response.body<String>()}")
        }

        val tokenResponse: TokenResponse = response.body()

        return BearerTokens(tokenResponse.access_token, tokenResponse.refresh_token ?: "")
    }
}

val DirectoryAuthPlugin =
    createClientPlugin("DirectoryAuthPlugin", ::DirectoryAuthPluginConfig) {

        var accessToken = runBlocking { pluginConfig.accessTokenCallback() }

        on(Send) { request ->
            request.headers.append("Authorization", "Bearer $accessToken")
            val originalCall = proceed(request)
            if (pluginConfig.retry && originalCall.response.status == HttpStatusCode.Unauthorized) {
                logger.debug { "Token was refused by the server. Probably expired, obtain new token and retry." }
                // try to obtain the accessToken again and retry
                accessToken = runBlocking { pluginConfig.accessTokenCallback() }
                request.headers["Authorization"] = "Bearer $accessToken"
                proceed(request)
            } else {
                originalCall
            }
        }
    }

class DirectoryAuthPluginConfig {
    var retry = true
    internal var accessTokenCallback: suspend () -> String? = { null }

    fun accessToken(block: suspend () -> String?) {
        accessTokenCallback = block
    }
}
