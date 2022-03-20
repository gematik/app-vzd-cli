package vzd.admin.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.groups.OptionGroup
import com.github.ajalt.clikt.parameters.groups.provideDelegate
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.prompt
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.choice
import com.github.ajalt.clikt.parameters.types.path
import mu.KotlinLogging
import vzd.admin.client.KeyStoreVaultProvider

private val logger = KotlinLogging.logger {}

class VaultCommand: CliktCommand(name = "vault", help = "Manage OAuth credentials in the Vault") {
    init {
        subcommands(
            VaultResetCommand(),
            VaultListCommand(),
            VaultStoreCommand(),
            VaultExportCommand(),
            VaultImportCommand(),
        )
    }

    override fun run() = Unit
}


class VaultCommonOptions: OptionGroup("Vault common options:") {
    val password by option("--password", "-p", help="Password for protection of the Vault")
        .prompt("Vault password", hideInput = true)
}

class VaultResetCommand: CliktCommand(name = "reset", help = "Reset all OAuth2 credentials") {
    private val vaultOptions by VaultCommonOptions()

    override fun run() = catching {
        KeyStoreVaultProvider(vaultOptions.password, reset = true)
    }
}

class VaultListCommand: CliktCommand(name = "list", help = "List configured OAuth2 credentials") {
    private val vaultOptions by VaultCommonOptions()

    override fun run() = catching {
        val provider = KeyStoreVaultProvider(vaultOptions.password)
        echo("Env ClientID             Secret")
        echo("=== ==================== ======")
        provider.list().forEach {
            echo("%-3s %-20s ******".format(it.environment, it.clientID))
        }
    }

}

class VaultStoreCommand: CliktCommand(name = "store", help = "Store OAuth2 client credentials") {
    private val vaultOptions by VaultCommonOptions()
    private val env by option("-e", "--env", help="Environment. Either tu, ru or pu").choice("tu", "ru", "pu")
        .prompt("Environment")
    private val clientID by option("-c", "--client-id", help="OAuth2 ClientID")
        .prompt("OAuth2 ClientID")
    private val secret by option("-s", "--secret", help="OAuth2 Client Secret")
        .prompt("OAuth2 Client Secret", hideInput = true)

    override fun run() = catching {
        val provider = KeyStoreVaultProvider(vaultOptions.password)
        provider.store(env, clientID, secret)
    }

}

class VaultExportCommand: CliktCommand(name = "export", help = "Export Vault to a file for backup or transfer.") {
    private val vaultOptions by VaultCommonOptions()
    private val output by option("-o", "--output").path(canBeDir = false).required()
    private val transferPassword by option("-t", "--transfer-password")
        .prompt("Enter Vault transfer password", hideInput = true)

    override fun run() = catching {
        logger.info { "Exporting Vault to $output"}
        val transferVault = KeyStoreVaultProvider(transferPassword, customVaultPath = output, reset = true)
        val vault = KeyStoreVaultProvider(vaultOptions.password)

        vault.list().forEach {
            logger.debug { "Export ${it.environment}:${it.clientID}" }
            transferVault.store(it.environment, it.clientID, it.secret)
        }

    }

}

class VaultImportCommand: CliktCommand(name = "import", help = "Import credentials from another Vault") {
    private val vaultOptions by VaultCommonOptions()
    private val input by option("-i", "--input").path(canBeDir = false, mustBeReadable = true).required()
    private val transferPassword by option("-t", "--transfer-password")
        .prompt("Enter Vault transfer password", hideInput = true)

    override fun run() = catching {
        logger.info { "Importing Vault from $input"}
        val transferVault = KeyStoreVaultProvider(transferPassword, customVaultPath = input)
        val vault = KeyStoreVaultProvider(vaultOptions.password)

        transferVault.list().forEach {
            logger.debug { "Import ${it.environment}:${it.clientID}" }
            vault.store(it.environment, it.clientID, it.secret)
        }
    }

}
