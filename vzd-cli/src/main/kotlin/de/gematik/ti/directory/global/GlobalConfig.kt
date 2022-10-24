package de.gematik.ti.directory.global

import de.gematik.ti.directory.util.FileObjectStore
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import java.nio.file.Path

@Serializable
data class HttpProxyConfig(
    var proxyURL: String,
    var enabled: Boolean = true
)

@Serializable
class GlobalConfig(
    var httpProxy: HttpProxyConfig
)

class GlobalConfigFileStore(customConfigPath: Path? = null) : FileObjectStore<GlobalConfig>(
    "directory-global.yaml",
    { GlobalConfig(HttpProxyConfig("foo", false)) },
    { yaml, stringValue -> yaml.decodeFromString(stringValue) },
    customConfigPath
)
