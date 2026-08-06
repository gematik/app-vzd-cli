package de.gematik.ti.directory.cli.fhir

import de.gematik.ti.directory.ClientCredentialsAuthenticator
import de.gematik.ti.directory.DirectoryAuthException
import de.gematik.ti.directory.DirectoryEnvironment
import de.gematik.ti.directory.cli.GlobalAPI
import de.gematik.ti.directory.cli.util.KeyStoreVault
import de.gematik.ti.directory.cli.util.KeyStoreVaultProvider
import de.gematik.ti.directory.cli.util.TokenStore
import de.gematik.ti.directory.fhir.Client
import de.gematik.ti.directory.fhir.DefaultConfig
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive

const val FDV_SEARCH_SERVICE_NAME = "urn:gematik:directory:fhir:fdv-search"

class FhirAPI(
    val globalAPI: GlobalAPI
) {
    val config = DefaultConfig

    fun createClient(env: DirectoryEnvironment): Client {
        val cfg = config.environment(env)
        val client =
            Client {
                envConfig = cfg
                auth {
                    accessTokenForRequest { request ->
                        val path = request.url.encodedPath
                        if (path.startsWith("/fdv")) {
                            retrieveAccessTokenFdv(env)
                        } else {
                            retrieveAccessTokenSearch(env)
                        }
                    }
                }
                if (globalAPI.config.httpProxy.enabled) {
                    httpProxyURL = globalAPI.config.httpProxy.proxyURL
                }
            }
        return client
    }

    fun openVaultFdv(vaultPassword: String): KeyStoreVault = KeyStoreVaultProvider().open(vaultPassword, FDV_SEARCH_SERVICE_NAME)

    fun storeAccessTokenSearch(
        env: DirectoryEnvironment,
        accessToken: String
    ) {
        val tokenStore = TokenStore()
        val envConfig = config.environment(env)
        tokenStore.addAccessToken(envConfig.search.apiURL, accessToken)
    }

    fun retrieveAccessTokenSearch(env: DirectoryEnvironment): String {
        val tokenStore = TokenStore()
        val envConfig = config.environment(env)
        return tokenStore
            .accessTokenFor(
                envConfig.search.apiURL,
            )?.accessToken ?: throw DirectoryAuthException("You are not logged in to environment (SearchAPI): $env")
    }

    fun storeAccessTokenFdv(
        env: DirectoryEnvironment,
        accessToken: String
    ) {
        val tokenStore = TokenStore()
        val envConfig = config.environment(env)
        tokenStore.addAccessToken(envConfig.fdv.apiURL, accessToken)
    }

    fun retrieveAccessTokenFdv(env: DirectoryEnvironment): String {
        val tokenStore = TokenStore()
        val envConfig = config.environment(env)
        return tokenStore
            .accessTokenFor(
                envConfig.fdv.apiURL,
            )?.accessToken ?: throw DirectoryAuthException("You are not logged in to environment (FDVSearchAPI): $env")
    }

    fun loginHolder(
        env: DirectoryEnvironment,
        holderAccessToken: String,
    ): Map<String, String> {
        val tokenStore = TokenStore()
        val envConfig = config.environment(env)

        val httpClient = createHttpClient()
        val authzResponse: JsonObject =
            runBlocking {
                val response =
                    httpClient.get(envConfig.holder.authorizationEndpoint) {
                        headers {
                            append("Authorization", "Bearer $holderAccessToken")
                        }
                    }

                if (response.status == HttpStatusCode.Forbidden) {
                    throw DirectoryAuthException("Holder login failed. Re-login as administrator.")
                } else if (response.status != HttpStatusCode.OK) {
                    val body: String = response.body()
                    throw DirectoryAuthException(
                        "Holder login failed: env:$env , url:${envConfig.fdv.authorizationEndpoint}, status:${response.status.value}, body:$body",
                    )
                }
                response.body()
            }
        if (authzResponse["access_token"] == null) {
            throw DirectoryAuthException("Login failed: env:$env , url:${envConfig.holder.authorizationEndpoint}")
        }
        val accessToken = authzResponse["access_token"]!!.jsonPrimitive.content
        tokenStore.addAccessToken(envConfig.holder.apiURL, accessToken)
        tokenStore.addAccessToken(envConfig.search.apiURL, accessToken)

        logger.info { "Login successful: env:$env , url:${envConfig.holder.authorizationEndpoint}" }
        return tokenStore.claimsFor(envConfig.holder.apiURL)!!
    }

    fun loginFdv(
        env: DirectoryEnvironment,
        clientID: String,
        clientSecret: String
    ): Map<String, String> {
        val tokenStore = TokenStore()
        val envConfig = config.environment(env)

        val auth =
            ClientCredentialsAuthenticator(
                envConfig.fdv.authenticationEndpoint,
                if (globalAPI.config.httpProxy.enabled) globalAPI.config.httpProxy.proxyURL else null,
            )
        val authResponse = runBlocking { auth.authenticate(clientID, clientSecret) }
        val token = authResponse.accessToken

        val httpClient = createHttpClient()
        val authzResponse: JsonObject =
            runBlocking {
                val response =
                    httpClient.get(envConfig.fdv.authorizationEndpoint) {
                        headers {
                            append("Authorization", "Bearer $token")
                        }
                    }

                if (response.status.value != 200) {
                    val body: String = response.body()
                    throw DirectoryAuthException(
                        "Login failed: env:$env , clientID:$clientID, url:${envConfig.fdv.authorizationEndpoint}, status:${response.status.value}, body:$body",
                    )
                }
                response.body()
            }

        if (authzResponse["access_token"] == null) {
            throw DirectoryAuthException("Login failed: env:$env , clientID:$clientID, url:${envConfig.fdv.apiURL}")
        }
        tokenStore.addAccessToken(envConfig.fdv.apiURL, authzResponse["access_token"]!!.jsonPrimitive.content)

        logger.info { "Login successful: env:$env , clientID:$clientID, url:${envConfig.fdv.apiURL}" }
        return tokenStore.claimsFor(envConfig.fdv.apiURL)!!
    }

    fun createHttpClient(): HttpClient =
        HttpClient(CIO) {
            install(ContentNegotiation) {
                json(
                    Json {
                        ignoreUnknownKeys = true
                    },
                )
            }
            if (globalAPI.config.httpProxy.enabled) {
                engine {
                    logger.debug { "Using proxy: ${globalAPI.config.httpProxy.proxyURL} for FHIR authorization" }
                    proxy = ProxyBuilder.http(globalAPI.config.httpProxy.proxyURL)
                }
            }
        }
}
