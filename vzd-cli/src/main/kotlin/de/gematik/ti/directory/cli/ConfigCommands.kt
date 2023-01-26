package de.gematik.ti.directory.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.optional
import com.github.ajalt.clikt.parameters.types.choice
import net.mamoe.yamlkt.Yaml
import java.net.URL

private val YAML = Yaml { encodeDefaultValues = false }

class ConfigCommand : CliktCommand(name = "config", help = "Manage configuration") {
    init {
        subcommands(ConfigGetCommand(), ConfigSetCommand(), ConfigResetCommand())
    }

    override fun run() = Unit
}

class ConfigResetCommand : CliktCommand(name = "reset", help = "Reset configuration to defaults") {
    override fun run() = catching {
        val globalAPI = GlobalAPI()
        val newConfig = globalAPI.resetConfig()
        echo(YAML.encodeToString(newConfig))
    }
}

val SET_PROPERTIES = mapOf(
    "httpProxy.proxyURL" to { config: GlobalConfig, value: String ->
        config.httpProxy.proxyURL = URL(value).toString()
    },
    "httpProxy.enabled" to { config: GlobalConfig, value: String -> config.httpProxy.enabled = value.toBoolean() },
    "updates.preReleasesEnabled" to { config: GlobalConfig, value: String -> config.updates.preReleasesEnabled = value.toBoolean() },
)

class ConfigSetCommand : CliktCommand(
    name = "set",
    help = """Set configuration properties:
        
            ```${SET_PROPERTIES.keys.sorted().joinToString("\n")}
            ```
            """,
) {
    private val property by argument().choice(SET_PROPERTIES)
    private val value by argument()
    override fun run() {
        val globalAPI = GlobalAPI()
        val config = globalAPI.config
        property(config, value)
        globalAPI.updateConfig()
        echo(YAML.encodeToString(config))
    }
}

val GET_PROPERTIES = mapOf(
    "httpProxy" to { config: GlobalConfig -> config.httpProxy },
    "httpProxy.proxyURL" to { config: GlobalConfig -> config.httpProxy.proxyURL },
    "httpProxy.enabled" to { config: GlobalConfig -> config.httpProxy.enabled },
    "updates" to { config: GlobalConfig -> config.updates },
)

class ConfigGetCommand : CliktCommand(
    name = "get",
    help = """Get configuration properties:
        
            ```${GET_PROPERTIES.keys.sorted().joinToString("\n")}
            ```
            """,
) {
    private val property by argument().choice(GET_PROPERTIES).optional()
    override fun run() {
        val globalAPI = GlobalAPI()
        val config = globalAPI.loadConfig()
        val value = property?.let { it(config) } ?: config
        echo(YAML.encodeToString(value))
    }
}
