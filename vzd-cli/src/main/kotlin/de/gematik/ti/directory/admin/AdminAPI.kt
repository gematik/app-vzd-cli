package de.gematik.ti.directory.admin

import de.gematik.ti.directory.global.GlobalAPI
import de.gematik.ti.directory.util.*
import kotlinx.serialization.Serializable
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

enum class AdminEnvironment {
    ru,
    tu,
    pu;
}

@Serializable
data class AdminEnvironmentStatus(
    val env: String,
    val accessTokenClaims: Map<String, String>?,
    val backendInfo: InfoObject?
)

@Serializable
data class AdminStatus(
    val environmentStatus: List<AdminEnvironmentStatus>
)

class AdminAPI(val globalAPI: GlobalAPI) {

    fun createClient(env: AdminEnvironment): Client {
        val tokenStore = TokenStore()
        val envConfig = config.environment(env)
        val client = Client {
            apiURL = envConfig.apiURL
            accessToken = tokenStore.accessTokenFor(envConfig.apiURL)?.accessToken ?: throw DirectoryAuthException("You are not logged in to environment: $env")
            if (globalAPI.config.httpProxy.enabled) {
                httpProxyURL = globalAPI.config.httpProxy.proxyURL
            }
        }
        return client
    }

    private fun loadConfig() = AdminConfigFileStore().config

    fun updateConfig() {
        val store = AdminConfigFileStore()
        store.config = config
        store.save()
        logger.info { "Configuration updated" }
    }

    fun resetConfig(): Config {
        val store = AdminConfigFileStore()
        return store.reset()
    }

    val config by lazy { loadConfig() }
    fun openVault(vaultPassword: String): KeyStoreVault {
        return KeyStoreVaultProvider().open(vaultPassword)
    }

    suspend fun status(includeBackendInfo: Boolean = false): AdminStatus {
        // force reload from file in case smth changed in between the requests
        val tokenStore = TokenStore()
        val config = loadConfig()
        tokenStore.removeExpired()
        val envInfoList = config.environments.map {
            val backendInfo = if (includeBackendInfo) {
                try {
                    createClient(enumValueOf(it.key.lowercase())).getInfo()
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
            envInfoList
        )
    }

    fun environmentConfig(env: AdminEnvironment): EnvironmentConfig {
        return config.environment(env)
    }

    fun login(env: AdminEnvironment, clientID: String, clientSecret: String): Map<String, String> {
        val tokenStore = TokenStore()
        val envcfg = environmentConfig(env)

        val auth = ClientCredentialsAuthenticator(
            envcfg.authURL,
            if (globalAPI.config.httpProxy.enabled) globalAPI.config.httpProxy.proxyURL else null
        )
        val authResponse = auth.authenticate(clientID, clientSecret)

        tokenStore.addAccessToken(envcfg.apiURL, authResponse.accessToken)

        logger.info { "Login successful: env:$env , clientID:$clientID" }
        return tokenStore.claimsFor(envcfg.apiURL)!!
    }

    suspend fun expandOCSPStatus(entries: List<DirectoryEntry>?) {
        entries?.mapNotNull { it.userCertificates }
            ?.flatten()
            ?.mapNotNull { it.userCertificate }
            ?.forEach {
                it.certificateInfo.ocspResponse = verifyCertificate(it)
            }
    }

    suspend fun verifyCertificate(cert: CertificateDataDER): OCSPResponse {
        return globalAPI.pkiClient.ocsp(cert)
    }
}
