package de.gematik.ti.directory.cli.admin

import de.gematik.ti.directory.DirectoryAuthException
import de.gematik.ti.directory.admin.*
import de.gematik.ti.directory.cli.GlobalAPI
import de.gematik.ti.directory.cli.util.FileObjectStore
import de.gematik.ti.directory.cli.util.KeyStoreVault
import de.gematik.ti.directory.cli.util.KeyStoreVaultProvider
import de.gematik.ti.directory.cli.util.TokenStore
import de.gematik.ti.directory.pki.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import mu.KotlinLogging
import java.nio.file.Path

private val logger = KotlinLogging.logger {}

typealias AdminEnvironment = de.gematik.ti.directory.admin.AdminEnvironment

internal class AdminConfigFileStore(customConfigPath: Path? = null) : FileObjectStore<Config>(
    "directory-admin.yaml",
    { DefaultConfig },
    { yaml, stringValue -> yaml.decodeFromString(stringValue) },
    customConfigPath,
) {
    var config: Config get() = value
        set(newValue) {
            value = newValue
        }
}

@Serializable
data class AdminEnvironmentStatus(
    val env: String,
    val accessTokenClaims: Map<String, String>?,
    val backendInfo: InfoObject?,
)

@Serializable
data class AdminStatus(
    val environmentStatus: List<AdminEnvironmentStatus>,
)

class AdminAPI(val globalAPI: GlobalAPI) {
    fun createClient(env: AdminEnvironment): Client {
        val tokenStore = TokenStore()
        val envConfig = config.environment(env)
        val client =
            Client {
                apiURL = envConfig.apiURL
                auth {
                    accessToken {
                        tokenStore.accessTokenFor(envConfig.apiURL)?.accessToken ?: throw DirectoryAuthException("You are not logged in to environment: $env")
                    }
                }
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
        val envInfoList =
            config.environments.map {
                val backendInfo =
                    if (includeBackendInfo) {
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
                    backendInfo,
                )
            }
        return AdminStatus(
            envInfoList,
        )
    }

    fun environmentConfig(env: AdminEnvironment): EnvironmentConfig {
        return config.environment(env)
    }

    fun login(
        env: AdminEnvironment,
        clientID: String,
        clientSecret: String,
    ): Map<String, String> {
        val tokenStore = TokenStore()
        val envConfig = environmentConfig(env)

        val auth =
            ClientCredentialsAuthenticator(
                envConfig.authURL,
                if (globalAPI.config.httpProxy.enabled) globalAPI.config.httpProxy.proxyURL else null,
            )
        val authResponse = runBlocking { auth.authenticate(clientID, clientSecret) }

        tokenStore.addAccessToken(envConfig.apiURL, authResponse.accessToken)

        logger.info { "Login successful: env:$env , clientID:$clientID" }
        return tokenStore.claimsFor(envConfig.apiURL)!!
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
