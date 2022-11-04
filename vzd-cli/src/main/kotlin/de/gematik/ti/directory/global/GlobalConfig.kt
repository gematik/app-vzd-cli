package de.gematik.ti.directory.global

import de.gematik.ti.directory.cli.BuildConfig
import de.gematik.ti.directory.util.FileObjectStore
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import java.nio.file.Path

@Serializable
data class HttpProxyConfig(
    var proxyURL: String,
    var enabled: Boolean,
)

@Serializable
data class GlobalConfig(
    var httpProxy: HttpProxyConfig,
    var updates: UpdatesConfig
)

@Serializable
data class UpdatesConfig(
    val checkURL: String,
    var preReleasesEnabled: Boolean,
    var lastCheck: Long,
    var latestRelease: String
)

internal class GlobalConfigFileStore(customConfigPath: Path? = null) : FileObjectStore<GlobalConfig>(
    "directory-global.yaml",
    {
        GlobalConfig(
            httpProxy = HttpProxyConfig(
                proxyURL = "http://192.168.110.10:3128/",
                enabled = false
            ),
            updates = UpdatesConfig(
                checkURL = "https://raw.githubusercontent.com/gematik/app-vzd-cli/main/latest.json",
                preReleasesEnabled = false,
                lastCheck = -1,
                latestRelease = BuildConfig.APP_VERSION
            ),
        )
    },
    { yaml, stringValue -> yaml.decodeFromString(stringValue) },
    customConfigPath
)
