package vzd.admin.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.CliktError
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.groups.provideDelegate
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.choice
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import mu.KotlinLogging
import net.mamoe.yamlkt.Yaml
import vzd.admin.client.ClientCredentialsAuthenticator
import vzd.admin.client.FileConfigProvider
import vzd.admin.client.KeyStoreVaultProvider
import vzd.admin.client.TokenConfig
import java.util.Base64

private val logger = KotlinLogging.logger {}

private val YAML = Yaml { encodeDefaultValues = false }

class ConfigCommand: CliktCommand(name = "config", help = "Manage configuration") {
    init {
        subcommands(ConfigViewCommand(), ConfigResetCommand())
    }

    override fun run() = Unit
}

class ConfigResetCommand: CliktCommand(name = "reset", help = "Reset configuration to defaults") {
    override fun run() = catching {
        val provider = FileConfigProvider()
        provider.reset()
        echo(YAML.encodeToString(provider.config))
    }
}


class ConfigViewCommand: CliktCommand(name = "view", help = "Show configuration") {
    override fun run() = catching {
        val provider = FileConfigProvider()
        echo(YAML.encodeToString(provider.config))
    }
}
