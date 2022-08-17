package de.gematik.ti.directory.admin.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.CliktError
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.optional
import com.github.ajalt.clikt.parameters.types.choice
import de.gematik.ti.directory.admin.client.Config
import de.gematik.ti.directory.admin.client.FileConfigProvider
import mu.KotlinLogging
import net.mamoe.yamlkt.Yaml
import java.net.URL

private val logger = KotlinLogging.logger {}

private val YAML = Yaml { encodeDefaultValues = false }

class ConfigCommand : CliktCommand(name = "config", help = "Manage configuration") {
    init {
        subcommands(ConfigGetCommand(), ConfigSetCommand(), ConfigResetCommand())
    }

    override fun run() = Unit
}

class ConfigResetCommand : CliktCommand(name = "reset", help = "Reset configuration to defaults") {
    override fun run() = catching {
        val provider = FileConfigProvider()
        provider.reset()
        echo(YAML.encodeToString(provider.config))
    }
}

val SET_PROPERTIES = mapOf<String, (Config, String) -> Unit>(
    "currentEnvironment" to { config: Config, value: String ->
        if (!config.environments.containsKey(value)) throw CliktError("Invalid environment name: $value")
        config.currentEnvironment = value
    },
    "httpProxy.proxyURL" to { config: Config, value: String ->
        config.httpProxy.proxyURL = URL(value).toString()
    },
    "httpProxy.enabled" to { config: Config, value: String -> config.httpProxy.enabled = value.toBoolean() }
)

class ConfigSetCommand : CliktCommand(
    name = "set",
    help = """Set configuration properties:
        
            ```${SET_PROPERTIES.keys.sorted().joinToString("\n")}
            ```
            """
) {
    private val property by argument().choice(SET_PROPERTIES)
    private val value by argument()
    override fun run() {
        val provider = FileConfigProvider()
        property(provider.config, value)
        provider.save()
        provider.config.tokens = null
        echo(YAML.encodeToString(provider.config))
    }
}

val GET_PROPERTIES = mapOf<String, (Config) -> Any?>(
    "environments" to { config: Config -> config.environments },
    "currentEnvironment" to { config: Config -> config.currentEnvironment },
    "httpProxy" to { config: Config -> config.httpProxy },
    "httpProxy.proxyURL" to { config: Config -> config.httpProxy.proxyURL },
    "httpProxy.enabled" to { config: Config -> config.httpProxy.enabled }
)

class ConfigGetCommand : CliktCommand(
    name = "get",
    help = """Get configuration properties:
        
            ```${GET_PROPERTIES.keys.sorted().joinToString("\n")}
            ```
            """
) {
    private val property by argument().choice(GET_PROPERTIES).optional()
    override fun run() {
        val provider = FileConfigProvider()

        provider.config.tokens = null
        val value = property?.let { it(provider.config) } ?: provider.config

        echo(YAML.encodeToString(value))
    }
}
