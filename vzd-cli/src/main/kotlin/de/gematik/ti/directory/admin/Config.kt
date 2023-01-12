package de.gematik.ti.directory.admin

import de.gematik.ti.directory.util.DirectoryException
import de.gematik.ti.directory.util.FileObjectStore
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import net.mamoe.yamlkt.Yaml
import java.nio.file.Path

private val YAML = Yaml { encodeDefaultValues = false }

internal class AdminConfigFileStore(customConfigPath: Path? = null) : FileObjectStore<Config>(
    "directory-admin.yaml",
    {
        Config(
            environments = mapOf(
                "tu" to EnvironmentConfig(
                    authURL = "https://auth-test.vzd.ti-dienste.de:9443/auth/realms/RSDirectoryAdministration/protocol/openid-connect/token",
                    apiURL = "https://vzdpflege-test.vzd.ti-dienste.de:9543",
                ),
                "ru" to EnvironmentConfig(
                    authURL = "https://auth-ref.vzd.ti-dienste.de:9443/auth/realms/RSDirectoryAdministration/protocol/openid-connect/token",
                    apiURL = "https://vzdpflege-ref.vzd.ti-dienste.de:9543/",
                ),
                "pu" to EnvironmentConfig(
                    authURL = "https://auth.vzd.ti-dienste.de:9443/auth/realms/RSDirectoryAdministration/protocol/openid-connect/token",
                    apiURL = "https://vzdpflege.vzd.ti-dienste.de:9543",
                ),
            ),
        )
    },
    { yaml, stringValue -> yaml.decodeFromString(stringValue) },
    customConfigPath,
) {
    var config: Config get() = value
        set(newValue) { value = newValue }
}

@Serializable
data class Config(
    val environments: Map<String, EnvironmentConfig>,
) {
    fun environment(env: AdminEnvironment) = environments[env.name] ?: throw ConfigException("Unknown environment: ${env.name}")
}

@Serializable
data class EnvironmentConfig(
    val authURL: String,
    val apiURL: String,
)

class ConfigException(message: String, cause: Throwable? = null) : DirectoryException(message, cause)
