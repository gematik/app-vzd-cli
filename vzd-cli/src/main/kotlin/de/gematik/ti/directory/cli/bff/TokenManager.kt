package de.gematik.ti.directory.cli.bff

import de.gematik.ti.directory.ClientCredentialsAuthenticator
import de.gematik.ti.directory.DirectoryEnvironment
import de.gematik.ti.directory.admin.DefaultConfig
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.time.Duration

/**
 * Token Manager manages the access tokens in backend, refreshing them when necessary.
 */
class TokenManager(
    var defaultExpiresIn: Duration,
    var httpProxyUrl: String? = null,
) {
    private val tokenProviders = HashMap<String, TokenProvider>()

    fun registerAdminCredentials(
        env: DirectoryEnvironment,
        clientId: String,
        clientSecret: String
    ) {
        val cfg = DefaultConfig.environment(env)
        val authenticator = ClientCredentialsAuthenticator(cfg.authURL, httpProxyUrl)
        tokenProviders[cfg.apiURL] = TokenProvider(authenticator, clientId, clientSecret, defaultExpiresIn)
    }

    fun accessTokenFor(apiURL: String): String? = tokenProviders.get(apiURL)?.accessToken()
}

class TokenProvider(
    private val auth: ClientCredentialsAuthenticator,
    private val clientId: String,
    private val clientSecret: String,
    private val expiresIn: Duration,
) {
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
