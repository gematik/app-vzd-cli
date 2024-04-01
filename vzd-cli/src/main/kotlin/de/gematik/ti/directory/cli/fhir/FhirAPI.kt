package de.gematik.ti.directory.cli.fhir

import de.gematik.ti.directory.ClientCredentialsAuthenticator
import de.gematik.ti.directory.DirectoryAuthException
import de.gematik.ti.directory.DirectoryEnvironment
import de.gematik.ti.directory.cli.GlobalAPI
import de.gematik.ti.directory.cli.util.FileObjectStore
import de.gematik.ti.directory.cli.util.KeyStoreVault
import de.gematik.ti.directory.cli.util.KeyStoreVaultProvider
import de.gematik.ti.directory.cli.util.TokenStore
import de.gematik.ti.directory.fhir.Client
import de.gematik.ti.directory.fhir.Config
import de.gematik.ti.directory.fhir.DefaultConfig
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.decodeFromString
import java.nio.file.Path

const val FDV_SEARCH_SERVICE_NAME = "urn:gematik:directory:fhir:fdv-search"

class FhirAPI(val globalAPI: GlobalAPI) {
    val config = DefaultConfig

    fun createClient(env: DirectoryEnvironment): Client {
        val cfg = config.environment(env)
        val client =
            Client {
                envConfig = cfg
                authFdv {
                    accessToken {
                        retrieveAccessTokenFdv(env)
                    }
                }
                authSearch {
                    accessToken {
                        retrieveAccessTokenSearch(env)
                    }
                }
                if (globalAPI.config.httpProxy.enabled) {
                    httpProxyURL = globalAPI.config.httpProxy.proxyURL
                }
            }
        return client
    }

    fun openVault(vaultPassword: String): KeyStoreVault {
        return KeyStoreVaultProvider().open(vaultPassword, FDV_SEARCH_SERVICE_NAME)
    }

    fun storeAccessTokenSearch(env: DirectoryEnvironment, accessToken: String) {
        val tokenStore = TokenStore()
        val envConfig = config.environment(env)
        tokenStore.addAccessToken(envConfig.search.apiURL, accessToken)
    }

    fun retrieveAccessTokenSearch(env: DirectoryEnvironment): String {
        val tokenStore = TokenStore()
        val envConfig = config.environment(env)
        return tokenStore.accessTokenFor(envConfig.search.apiURL)?.accessToken ?: throw DirectoryAuthException("You are not logged in to environment (SearchAPI): $env")
    }

    fun storeAccessTokenFdv(env: DirectoryEnvironment, accessToken: String) {
        val tokenStore = TokenStore()
        val envConfig = config.environment(env)
        tokenStore.addAccessToken(envConfig.fdv.apiURL, accessToken)
    }

    fun retrieveAccessTokenFdv(env: DirectoryEnvironment): String {
        val tokenStore = TokenStore()
        val envConfig = config.environment(env)
        return tokenStore.accessTokenFor(envConfig.fdv.apiURL)?.accessToken ?: throw DirectoryAuthException("You are not logged in to environment (FDVSearchAPI): $env")
    }

    fun loginFdv(env: DirectoryEnvironment, clientID: String, clientSecret: String): Map<String, String> {
        val tokenStore = TokenStore()
        val envConfig = config.environment(env)

        val auth = ClientCredentialsAuthenticator(
            envConfig.fdv.authenticationEndpoint,
            if (globalAPI.config.httpProxy.enabled) globalAPI.config.httpProxy.proxyURL else null,
        )
        val authResponse = runBlocking { auth.authenticate(clientID, clientSecret) }

        tokenStore.addAccessToken(envConfig.fdv.apiURL, authResponse.accessToken)

        logger.info { "Login successful: env:$env , clientID:$clientID, url:${envConfig.fdv.apiURL}" }
        return tokenStore.claimsFor(envConfig.fdv.apiURL)!!
    }

}