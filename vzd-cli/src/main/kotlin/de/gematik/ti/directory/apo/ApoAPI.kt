package de.gematik.ti.directory.apo

import de.gematik.ti.directory.global.GlobalAPI
import de.gematik.ti.directory.util.DirectoryException
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

// ktlint-disable enum-entry-name-case
enum class ApoInstance {
    test,
    prod,
    ;

    fun lowercase(): String {
        return toString().lowercase()
    }
}

class ApoAPI(val globalAPI: GlobalAPI) {

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
