package vzd.admin.client

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import net.mamoe.yamlkt.Yaml
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.readText
import kotlin.io.path.writeText

private val YAML = Yaml { encodeDefaultValues = false }

class FileConfigProvider(val customConfigPath: Path? = null) {
    private val defaultConfigPath = Path(System.getProperty("user.home"), ".telematik", "directory-admin.yaml")
    val configPath get() = customConfigPath ?: defaultConfigPath
    var config: Config;

    init {
        if (!configPath.toFile().exists()) {
            configPath.parent.toFile().mkdirs()
        }
        if (configPath.toFile().exists()) {
            config = YAML.decodeFromString(configPath.readText())
        } else {
            config = DefaultConfig
            save()
        }
    }


    fun reset(): Config {
        config = DefaultConfig
        save()
        return config
    }

    fun save() {
        configPath.writeText(YAML.encodeToString(config))
    }

}

val DefaultConfig = Config(
    environments = mapOf(
        "tu" to EnvironmentConfig(
            authURL = "https://auth-test.vzd.ti-dienste.de:9443/auth/realms/RSDirectoryAdministration/protocol/openid-connect/token",
            apiURL = "https://vzdpflege-test.vzd.ti-dienste.de:9543"
        ),
        "ru" to EnvironmentConfig(
            authURL = "https://auth-ref.vzd.ti-dienste.de:9443/auth/realms/RSDirectoryAdministration/protocol/openid-connect/token",
            apiURL = "https://vzdpflege-ref.vzd.ti-dienste.de:9543/"
        ),
        "pu" to EnvironmentConfig(
            authURL = "https://auth.vzd.ti-dienste.de:9443/auth/realms/RSDirectoryAdministration/protocol/openid-connect/token",
            apiURL = "https://vzdpflege.vzd.ti-dienste.de:9543"
        )
    ),
    currentEnvironment = "ru",
    httpProxy = HttpProxyConfig(
        proxyURL = "http://192.168.110.10:3128/",
        enabled = false
    )
)

@Serializable
data class Config(
    var environments: Map<String,EnvironmentConfig>,
    var currentEnvironment: String?,
    var tokens: Map<String,TokenConfig>? = null,
    var httpProxy: HttpProxyConfig? = null
) {
    fun environment(name: String? = null) = environments.get(name ?: currentEnvironment)
}

@Serializable
data class TokenConfig(
    var accessToken: String?
)

@Serializable
data class EnvironmentConfig(
    var authURL: String,
    var apiURL: String
)

@Serializable
data class HttpProxyConfig(
    var proxyURL: String,
    var enabled: Boolean = true
)