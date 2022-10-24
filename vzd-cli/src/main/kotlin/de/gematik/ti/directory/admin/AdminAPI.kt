package de.gematik.ti.directory.admin

import de.gematik.ti.directory.util.DirectoryAuthException
import de.gematik.ti.directory.util.TokenStore
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

enum class AdminEnvironment(val title: String) {
    RU("ru"),
    TU("tu"),
    PU("pu")
}

@Serializable
data class AdminEnvironmentStatus(
    val env: String,
    val accessTokenClaims: JsonObject?,
    val backendInfo: InfoObject?
)

@Serializable
data class AdminStatus(
    val config: Config,
    val environmentStatus: List<AdminEnvironmentStatus>
)

class AdminAPI() {

    fun createClient(env: AdminEnvironment): Client {
        val tokenStore = TokenStore()
        val envConfig = config.environment(env.title)
        val client = Client {
            apiURL = envConfig.apiURL
            accessToken = tokenStore.accessTokenFor(envConfig.apiURL)?.accessToken ?: throw DirectoryAuthException("You are not logged in to environment: ${env.title}")
            if (config.httpProxy.enabled) {
                httpProxyURL = config.httpProxy.proxyURL
            }
        }
        return client
    }

    private fun loadConfig() = FileConfigProvider().config

    fun updateConfig() {
        val configProvider = FileConfigProvider()
        configProvider.config = config
        configProvider.save()
        logger.info { "Configuration updated" }
    }

    val config by lazy { loadConfig() }

    suspend fun status(includeBackendInfo: Boolean = false): AdminStatus {
        // force reload from file in case smth changed in between the requests
        val tokenStore = TokenStore()
        val config = loadConfig()
        tokenStore.removeExpired()
        val envInfoList = config.environments.map {
            val backendInfo = if (includeBackendInfo) {
                try {
                    createClient(enumValueOf(it.key.uppercase())).getInfo()
                } catch (e: Exception) {
                    null
                }
            } else {
                null
            }
            AdminEnvironmentStatus(
                it.key,
                tokenStore.claimsFor(it.value.apiURL),
                backendInfo
            )
        }
        return AdminStatus(
            config,
            envInfoList
        )
    }

    fun environmentConfig(env: AdminEnvironment): EnvironmentConfig {
        return config.environment(env.title)
    }

    fun login(env: AdminEnvironment, clientID: String, clientSecret: String): JsonObject {
        val tokenStore = TokenStore()
        val envcfg = environmentConfig(env)

        val auth = ClientCredentialsAuthenticator(
            envcfg.authURL,
            if (config.httpProxy.enabled) config.httpProxy.proxyURL else null
        )
        val authResponse = auth.authenticate(clientID, clientSecret)

        tokenStore.addAccessToken(envcfg.apiURL, authResponse.accessToken)

        logger.info { "Login successful: env:$env , clientID:$clientID" }
        return tokenStore.claimsFor(envcfg.apiURL)!!
    }
}
