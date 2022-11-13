package de.gematik.ti.directory.cli.admin

import com.github.ajalt.clikt.core.CliktCommand
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
        subcommands(ConfigGetCommand(), ConfigResetCommand())
    }

    override fun run() = Unit
}

class ConfigResetCommand : CliktCommand(name = "reset", help = "Reset configuration to defaults") {
    private val context by requireObject<AdminCliEnvironmentContext>()
    override fun run() = catching {
        val config = context.adminAPI.resetConfig()
        echo(YAML.encodeToString(config))
    }
}

/*
val SET_PROPERTIES: Map<String, (Config, String) -> Unit> = emptyMap()

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
*/

val GET_PROPERTIES = mapOf(
    "environments" to { config: Config -> config.environments },
    "environments.pu" to { config: Config -> config.environments["pu"] },
    "environments.ru" to { config: Config -> config.environments["ru"] },
    "environments.tu" to { config: Config -> config.environments["tu"] }
)

class ConfigGetCommand : CliktCommand(
    name = "get",
    help = """Get configuration properties:
        
            ```${GET_PROPERTIES.keys.sorted().joinToString("\n")}
            ```
            """
) {
    private val context by requireObject<AdminCliEnvironmentContext>()
    private val property by argument().choice(GET_PROPERTIES).optional()
    override fun run() {
        val config = context.adminAPI.config
        val value = property?.let { it(config) } ?: config
        echo(YAML.encodeToString(value))
    }
}
