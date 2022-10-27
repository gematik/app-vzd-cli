package de.gematik.ti.directory.cli.admin

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.CliktError
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.output.TermUi
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.prompt
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.choice
import com.github.ajalt.clikt.parameters.types.path
import de.gematik.ti.directory.cli.catching
import de.gematik.ti.directory.util.KeyStoreVault
import de.gematik.ti.directory.util.KeyStoreVaultProvider
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

class VaultCommand : CliktCommand(name = "vault", help = "Manage OAuth credentials in the Vault") {
    init {
        subcommands(
            VaultResetCommand(),
            VaultListCommand(),
            VaultStoreCommand(),
            VaultExportCommand(),
            VaultImportCommand()
        )
    }

    override fun run() = Unit
}

val vaultProvider = KeyStoreVaultProvider()

fun openOrCreateVault(password: String?): KeyStoreVault {
    return password?.let {
        vaultProvider.open(password)
    } ?: run {
        if (vaultProvider.exists()) {
            val promptPassword = TermUi.prompt("Enter Vault password", hideInput = true) ?: throw CliktError()
            vaultProvider.open(promptPassword)
        } else {
            logger.info { "Creating new vault" }
            val newPassword = TermUi.prompt("Creating new Vault. Enter new Vault password", hideInput = true, requireConfirmation = true) ?: throw CliktError()
            vaultProvider.open(newPassword)
        }
    }
}

class VaultResetCommand : CliktCommand(name = "purge", help = "Remove Vault") {
    override fun run() = catching {
        vaultProvider.purge()
    }
}

class VaultListCommand : CliktCommand(name = "list", help = "List configured OAuth2 credentials") {
    private val password by option("--password", "-p", help = "Password for protection of the Vault")

    override fun run() = catching {
        val vault = openOrCreateVault(password)
        echo("Env ClientID             Secret")
        echo("=== ==================== ======")
        vault.list().forEach {
            echo("%-3s %-20s ******".format(it.variant, it.name))
        }
    }
}

class VaultStoreCommand : CliktCommand(name = "store", help = "Store OAuth2 client credentials") {
    private val password by option("--password", "-p", help = "Password for protection of the Vault")
    private val env by option("-e", "--env", help = "Environment. Either tu, ru or pu").choice("tu", "ru", "pu")
        .prompt("Environment")
    private val clientID by option("-c", "--client-id", help = "OAuth2 ClientID")
        .prompt("OAuth2 ClientID")
    private val secret by option("-s", "--secret", help = "OAuth2 Client Secret")
        .prompt("OAuth2 Client Secret", hideInput = true)

    override fun run() = catching {
        val vault = openOrCreateVault(password)
        vault.store(env, clientID, secret)
    }
}

class VaultExportCommand : CliktCommand(name = "export", help = "Export Vault to a file for backup or transfer.") {
    private val password by option("--password", "-p", help = "Password for protection of the Vault")
    private val output by option("-o", "--output").path(canBeDir = false).required()
    private val transferPassword by option("-t", "--transfer-password")
        .prompt("Enter Vault transfer password", hideInput = true)

    override fun run() = catching {
        logger.info { "Exporting Vault to $output" }
        val transferVaultProvider = KeyStoreVaultProvider(customVaultPath = output)
        val transferVault = transferVaultProvider.open(transferPassword)
        val vault = openOrCreateVault(password)

        vault.list().forEach {
            logger.info { "Exporting ${it.variant}:${it.name}" }
            transferVault.store(it.variant, it.name, it.secret)
        }
    }
}

class VaultImportCommand : CliktCommand(name = "import", help = "Import credentials from another Vault") {
    private val password by option("--password", "-p", help = "Password for protection of the Vault")
    private val input by option("-i", "--input").path(canBeDir = false, mustBeReadable = true).required()
    private val transferPassword by option("-t", "--transfer-password")
        .prompt("Enter TRANSFER Vault password", hideInput = true)

    override fun run() = catching {
        logger.info { "Importing Vault from $input" }
        val transferVault = KeyStoreVaultProvider(customVaultPath = input).open(transferPassword)
        val vault = openOrCreateVault(password)

        transferVault.list().forEach {
            logger.debug { "Import ${it.variant}:${it.name}" }
            vault.store(it.variant, it.name, it.secret)
        }
    }
}
