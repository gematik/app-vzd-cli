package de.gematik.ti.directory.cli.admin

import de.gematik.ti.directory.ClientCredentialsAuthenticator
import de.gematik.ti.directory.DirectoryAuthException
import de.gematik.ti.directory.DirectoryEnvironment
import de.gematik.ti.directory.admin.*
import de.gematik.ti.directory.cli.GlobalAPI
import de.gematik.ti.directory.cli.bff.TokenProvider
import de.gematik.ti.directory.cli.bff.TokenStoreTokenProvider
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

const val SERVICE_NAME = "urn:gematik:directory:admin"

typealias AdminEnvironment = de.gematik.ti.directory.DirectoryEnvironment

internal class AdminConfigFileStore(
    customConfigPath: Path? = null
) : FileObjectStore<Config>(
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
    val accessible: Boolean,
    val backendInfo: InfoObject?,
)

@Serializable
data class AdminStatus(
    val environmentStatus: List<AdminEnvironmentStatus>,
)

class AdminAPI(
    val globalAPI: GlobalAPI
) {
    val config by lazy { loadConfig() }

    var tokenProvider: TokenProvider = TokenStoreTokenProvider()

    fun createClient(env: AdminEnvironment): Client {
        val envConfig = config.environment(env)
        val client =
            Client {
                apiURL = envConfig.apiURL
                auth {
                    accessToken {
                        tokenProvider.accessTokenFor(envConfig.apiURL) ?: throw DirectoryAuthException("You are not logged in to environment: $env")
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

    fun openVault(vaultPassword: String): KeyStoreVault = KeyStoreVaultProvider().open(vaultPassword, SERVICE_NAME)

    suspend fun status(includeBackendInfo: Boolean = false): AdminStatus {
        tokenProvider.updateTokens()
        // force reload from file in case smth changed in between the requests
        val config = loadConfig()
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
                val accessible =
                    try {
                        tokenProvider.accessTokenFor(it.value.apiURL) != null
                    } catch (e: Exception) {
                        logger.error(e) { "Error while checking environment status: ${it.key}" }
                        false
                    }
                AdminEnvironmentStatus(
                    it.key,
                    accessible,
                    backendInfo,
                )
            }
        return AdminStatus(
            envInfoList,
        )
    }

    fun environmentConfig(env: AdminEnvironment): EnvironmentConfig = config.environment(env)

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
        entries
            ?.mapNotNull { it.userCertificates }
            ?.flatten()
            ?.mapNotNull { it.userCertificate }
            ?.forEach {
                it.certificateInfo.ocspResponse = verifyCertificate(it)
            }
    }

    suspend fun verifyCertificate(cert: CertificateDataDER): OCSPResponse = globalAPI.pkiClient.ocsp(cert)
}
