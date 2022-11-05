package de.gematik.ti.directory.cli.admin

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.CliktError
import com.github.ajalt.clikt.core.requireObject
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.optional
import com.github.ajalt.clikt.parameters.types.choice
import de.gematik.ti.directory.admin.Config
import de.gematik.ti.directory.cli.catching
import net.mamoe.yamlkt.Yaml

private val YAML = Yaml { encodeDefaultValues = false }

class ConfigCommand : CliktCommand(name = "config", help = "Manage configuration") {
    init {
        subcommands(ConfigGetCommand(), ConfigSetCommand(), ConfigResetCommand())
    }

    override fun run() = Unit
}

class ConfigResetCommand : CliktCommand(name = "reset", help = "Reset configuration to defaults") {
    private val context by requireObject<CommandContext>()
    override fun run() = catching {
        val config = context.adminAPI.resetConfig()
        echo(YAML.encodeToString(config))
    }
}

val SET_PROPERTIES = mapOf(
    "currentEnvironment" to { config: Config, value: String ->
        if (!config.environments.containsKey(value)) throw CliktError("Invalid environment name: $value")
        config.currentEnvironment = value
    }
)

class ConfigSetCommand : CliktCommand(
    name = "set",
    help = """Set configuration properties:
        
            ```${SET_PROPERTIES.keys.sorted().joinToString("\n")}
            ```
            """
) {
    private val context by requireObject<CommandContext>()
    private val property by argument().choice(SET_PROPERTIES)
    private val value by argument()
    override fun run() {
        val config = context.adminAPI.config
        property(config, value)
        context.adminAPI.updateConfig()
        echo(YAML.encodeToString(config))
    }
}

val GET_PROPERTIES = mapOf(
    "environments" to { config: Config -> config.environments },
    "currentEnvironment" to { config: Config -> config.currentEnvironment }
)

class ConfigGetCommand : CliktCommand(
    name = "get",
    help = """Get configuration properties:
        
            ```${GET_PROPERTIES.keys.sorted().joinToString("\n")}
            ```
            """
) {
    private val context by requireObject<CommandContext>()
    private val property by argument().choice(GET_PROPERTIES).optional()
    override fun run() {
        val config = context.adminAPI.config
        val value = property?.let { it(config) } ?: config
        echo(YAML.encodeToString(value))
    }
}