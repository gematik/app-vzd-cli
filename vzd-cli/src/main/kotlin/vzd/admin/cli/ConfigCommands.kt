package vzd.admin.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import kotlinx.serialization.encodeToString
import mu.KotlinLogging
import net.mamoe.yamlkt.Yaml
import vzd.admin.client.FileConfigProvider

private val logger = KotlinLogging.logger {}

private val YAML = Yaml { encodeDefaultValues = false }

class ConfigCommand : CliktCommand(name = "config", help = "Manage configuration") {
    init {
        subcommands(ConfigViewCommand(), ConfigResetCommand())
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

class ConfigViewCommand : CliktCommand(name = "view", help = "Show configuration") {
    override fun run() = catching {
        val provider = FileConfigProvider()
        echo(YAML.encodeToString(provider.config))
    }
}
