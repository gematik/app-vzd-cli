package de.gematik.ti.directory.cli.bff

import de.gematik.ti.directory.ClientCredentialsAuthenticator
import de.gematik.ti.directory.DirectoryEnvironment
import de.gematik.ti.directory.admin.DefaultConfig
import de.gematik.ti.directory.cli.util.TokenStore
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.time.Duration

/**
 * Interface token manager
 */
interface TokenProvider {
    fun accessTokenFor(apiURL: String): String?

    fun updateTokens()
}

/**
 * Token provider which uses the file based TokenStore
 */
class TokenStoreTokenProvider : TokenProvider {
    override fun accessTokenFor(apiURL: String): String? {
        val tokenStore = TokenStore()
        return tokenStore.accessTokenFor(apiURL)?.accessToken
    }

    override fun updateTokens() {
        val tokenStore = TokenStore()
        tokenStore.removeExpired()
    }
}

/**
 * Token Manager manages the access tokens in backend, refreshing them when necessary.
 */
class ClientCredentialsTokenProvider(
    var defaultExpiresIn: Duration,
    var httpProxyUrl: String? = null,
) : TokenProvider {
    private val authenticators = HashMap<String, Authenticator>()

    fun registerAdminCredentials(
        env: DirectoryEnvironment,
        clientId: String,
        clientSecret: String
    ) {
        logger.info { "Registering admin credentials for environment '$env', client_id: '$clientId'" }
        val cfg = DefaultConfig.environment(env)
        authenticators[cfg.apiURL] = Authenticator(cfg.authURL, httpProxyUrl, clientId, clientSecret, defaultExpiresIn)
    }

    override fun accessTokenFor(apiURL: String): String? = authenticators.get(apiURL)?.accessToken()

    override fun updateTokens() {
        // authenticators update automatically
    }
}

/**
 * Authenticator for client credentials
 */
class Authenticator(
    tokenURL: String,
    httpProxyUrl: String?,
    private val clientId: String,
    private val clientSecret: String,
    private val expiresIn: Duration,
) {
    private val auth = ClientCredentialsAuthenticator(tokenURL, httpProxyUrl)
    private var accessToken: String? = null
    private var expiresAt: Instant = Clock.System.now()

    fun accessToken(): String? {
        if (expiresAt <= Clock.System.now()) {
            runBlocking {
                authenticate()
            }
        }
        return accessToken
    }

    private suspend fun authenticate() {
        accessToken = auth.authenticate(clientId, clientSecret).accessToken
        expiresAt = Clock.System.now() + expiresIn
    }
}
