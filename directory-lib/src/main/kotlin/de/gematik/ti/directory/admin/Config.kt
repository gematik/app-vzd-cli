package de.gematik.ti.directory.admin

import de.gematik.ti.directory.DirectoryEnvironment
import de.gematik.ti.directory.DirectoryException
import kotlinx.serialization.Serializable

val DefaultConfig =
    Config(
        environments =
            mapOf(
                "tu" to
                    EnvironmentConfig(
                        authURL = "https://auth-test.vzd.ti-dienste.de:9443/auth/realms/RSDirectoryAdministration/protocol/openid-connect/token",
                        apiURL = "https://vzdpflege-test.vzd.ti-dienste.de:9543",
                    ),
                "ru" to
                    EnvironmentConfig(
                        authURL = "https://auth-ref.vzd.ti-dienste.de:9443/auth/realms/RSDirectoryAdministration/protocol/openid-connect/token",
                        apiURL = "https://vzdpflege-ref.vzd.ti-dienste.de:9543/",
                    ),
                "pu" to
                    EnvironmentConfig(
                        authURL = "https://auth.vzd.ti-dienste.de:9443/auth/realms/RSDirectoryAdministration/protocol/openid-connect/token",
                        apiURL = "https://vzdpflege.vzd.ti-dienste.de:9543",
                    ),
            ),
    )

class ConfigException(
    message: String,
    cause: Throwable? = null
) : DirectoryException(message, cause)

@Serializable
data class Config(
    val environments: Map<String, EnvironmentConfig>,
) {
    fun environment(env: DirectoryEnvironment) = environments[env.name] ?: throw ConfigException("Unknown environment: ${env.name}")
}

@Serializable
data class EnvironmentConfig(
    val authURL: String,
    val apiURL: String,
)
