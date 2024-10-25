package de.gematik.ti.directory.apo

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.requireObject
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.optional
import com.github.ajalt.clikt.parameters.types.choice
import de.gematik.ti.directory.cli.apo.ApoConfig
import de.gematik.ti.directory.cli.apo.ApoInstance
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
    private val context by requireObject<ApoCliContext>()

    override fun run() =
        catching {
            val newConfig = context.apoAPI.resetConfig()
            echo(YAML.encodeToString(newConfig))
        }
}

val SET_PROPERTIES =
    mapOf(
        "apiKeys.test" to { config: ApoConfig, value: String ->
            config.apiKeys = config.apiKeys + Pair(ApoInstance.test, value)
        },
        "apiKeys.prod" to { config: ApoConfig, value: String ->
            config.apiKeys = config.apiKeys + Pair(ApoInstance.prod, value)
        },
    )

class ConfigSetCommand :
    CliktCommand(
        name = "set",
        help = """Set configuration properties:
        
            ```${SET_PROPERTIES.keys.sorted().joinToString("\n")}
            ```
            """,
    ) {
    private val context by requireObject<ApoCliContext>()
    private val property by argument().choice(SET_PROPERTIES)
    private val value by argument()

    override fun run() {
        val config = context.apoAPI.config
        property(config, value)
        context.apoAPI.updateConfig()
        echo(YAML.encodeToString(config))
    }
}

val GET_PROPERTIES =
    mapOf(
        "emvironments" to { config: ApoConfig -> config.environments },
        "emvironments.test" to { config: ApoConfig -> config.environments[ApoInstance.test] },
        "emvironments.prod" to { config: ApoConfig -> config.environments[ApoInstance.prod] },
        "apiKeys" to { config: ApoConfig -> config.apiKeys },
        "apiKeys.test" to { config: ApoConfig -> config.apiKeys[ApoInstance.test] },
        "apiKeys.prod" to { config: ApoConfig -> config.apiKeys[ApoInstance.test] },
    )

class ConfigGetCommand :
    CliktCommand(
        name = "get",
        help = """Get configuration properties:
        
            ```${GET_PROPERTIES.keys.sorted().joinToString("\n")}
            ```
            """,
    ) {
    private val context by requireObject<ApoCliContext>()
    private val property by argument().choice(GET_PROPERTIES).optional()

    override fun run() {
        val config = context.apoAPI.config
        val value = property?.let { it(config) } ?: config
        echo(YAML.encodeToString(value))
    }
}
