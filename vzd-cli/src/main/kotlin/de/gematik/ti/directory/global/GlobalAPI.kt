package de.gematik.ti.directory.global

import de.gematik.ti.directory.util.PKIClient

class GlobalAPI {
    val config by lazy { loadConfig() }
    fun loadConfig(): GlobalConfig {
        return GlobalConfigFileStore().value
    }
    fun updateConfig() {
        val store = GlobalConfigFileStore()
        store.value = config
        store.save()
    }
    fun resetConfig() : GlobalConfig {
        val store = GlobalConfigFileStore()
        return store.reset()
    }

    val pkiClient by lazy {
        PKIClient {
            if (config.httpProxy.enabled) {
                httpProxyURL = config.httpProxy.proxyURL
            }
        }
    }
}
