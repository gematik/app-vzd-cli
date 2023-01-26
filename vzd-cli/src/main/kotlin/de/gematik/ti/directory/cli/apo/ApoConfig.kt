package de.gematik.ti.directory.cli.apo

import de.gematik.ti.directory.cli.util.FileObjectStore
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import java.nio.file.Path

@Serializable
data class ApoEnvironmentConfig(
    var apiURL: String,
)

@Serializable
class ApoConfig(
    val environments: Map<ApoInstance, ApoEnvironmentConfig>,
    var apiKeys: Map<ApoInstance, String>,
)

internal class ApoConfigFileStore(customConfigPath: Path? = null) : FileObjectStore<ApoConfig>(
    "directory-apo.yaml",
    {
        ApoConfig(
            mapOf(
                ApoInstance.test to ApoEnvironmentConfig("https://apovzd-test.app.ti-dienste.de/api/"),
                ApoInstance.prod to ApoEnvironmentConfig("https://apovzd.app.ti-dienste.de/api/"),
            ),
            mapOf(
                ApoInstance.test to "unknown",
                ApoInstance.prod to "unknown",
            ),
        )
    },
    { yaml, stringValue -> yaml.decodeFromString(stringValue) },
    customConfigPath,
)
