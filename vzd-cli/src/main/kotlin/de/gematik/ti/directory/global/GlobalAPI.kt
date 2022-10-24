package de.gematik.ti.directory.global

class GlobalAPI {
    fun loadConfig(): GlobalConfig {
        return GlobalConfigFileStore().value
    }
    fun saveConfig(globalConfig: GlobalConfig) {
        val store = GlobalConfigFileStore()
        store.value = globalConfig
        store.save()
    }
}
