package de.gematik.ti.directory.cli.apo

import de.gematik.ti.directory.DirectoryException
import de.gematik.ti.directory.apo.ApoClient
import de.gematik.ti.directory.cli.GlobalAPI
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

@Suppress("ktlint:standard:enum-entry-name-case")
enum class ApoInstance {
    test,
    prod,
}

class ApoAPI(
    val globalAPI: GlobalAPI
) {
    private fun loadConfig() = ApoConfigFileStore().value

    fun updateConfig() {
        val store = ApoConfigFileStore()
        store.value = config
        store.save()
        logger.info { "Configuration updated" }
    }

    fun resetConfig(): ApoConfig {
        val store = ApoConfigFileStore()
        return store.reset()
    }

    val config by lazy { loadConfig() }

    fun createClient(inst: ApoInstance): ApoClient {
        val envcfg = config.environments[inst] ?: throw DirectoryException("Unknown ApoVZD instance: $inst")
        val apiKey = config.apiKeys[inst] ?: throw DirectoryException("API Key is not available for ApoVZD instance: $inst")
        return ApoClient {
            this.apiURL = envcfg.apiURL
            this.apiKey = apiKey
            if (globalAPI.config.httpProxy.enabled) {
                this.httpProxyURL = globalAPI.config.httpProxy.proxyURL
            }
        }
    }
}
