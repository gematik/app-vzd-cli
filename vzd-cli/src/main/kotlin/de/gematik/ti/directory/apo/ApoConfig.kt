package de.gematik.ti.directory.apo

import de.gematik.ti.directory.util.FileObjectStore
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import java.nio.file.Path

@Serializable
data class ApoEnvironmentConfig(
    var apiURL: String
)

@Serializable
class ApoConfig(
    val environments: Map<String, ApoEnvironmentConfig>,
    var apiKeys: Map<String, String>
)

internal class ApoConfigFileStore(customConfigPath: Path? = null) : FileObjectStore<ApoConfig>(
    "directory-apo.yaml",
    {
        ApoConfig(
            mapOf(
                "test" to ApoEnvironmentConfig("https://apovzd-test.app.ti-dienste.de/api/"),
                "prod" to ApoEnvironmentConfig("https://apovzd.app.ti-dienste.de/api/")
            ),
            mapOf(
                "test" to "unknown",
                "prod" to "unknown"
            )
        )
    },
    { yaml, stringValue -> yaml.decodeFromString(stringValue) },
    customConfigPath
)
