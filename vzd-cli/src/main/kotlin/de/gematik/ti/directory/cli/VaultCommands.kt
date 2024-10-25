package de.gematik.ti.directory.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.CliktError
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.prompt
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.enum
import com.github.ajalt.clikt.parameters.types.path
import de.gematik.ti.directory.cli.admin.AdminEnvironment
import de.gematik.ti.directory.cli.util.KeyStoreVault
import de.gematik.ti.directory.cli.util.KeyStoreVaultProvider
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

class VaultCommand(
    serviceName: String,
    commandName: String = "vault",
    commandHelp: String = "Manage OAuth credentials in the Vault"
) : CliktCommand(
        name = commandName,
        help = commandHelp,
    ) {
    init {
        subcommands(
            VaultListCommand(serviceName),
            VaultStoreCommand(serviceName),
            VaultExportCommand(serviceName),
            VaultImportCommand(serviceName),
            VaultClearCommand(serviceName),
        )
    }

    override fun run() = Unit
}

abstract class AbstractVaultCommand(
    val serviceName: String,
    name: String,
    help: String
) : CliktCommand(name = name, help = help) {
    protected val password by option(
        "--password",
        "-p",
        help = "Password for protection of the Vault",
        envvar = "VAULT_PASSWORD",
    )

    protected val vaultProvider = KeyStoreVaultProvider()

    fun openOrCreateVault(): KeyStoreVault =
        password?.let {
            vaultProvider.open(it, serviceName)
        } ?: run {
            if (vaultProvider.exists()) {
                val promptPassword = prompt("Enter Vault password", hideInput = true) ?: throw CliktError()
                vaultProvider.open(promptPassword, serviceName)
            } else {
                logger.info { "Creating new vault" }
                val newPassword =
                    prompt(
                        "*** Creating new Vault.\nEnter new Vault password",
                        hideInput = true,
                        requireConfirmation = true,
                    ) ?: throw CliktError()
                vaultProvider.open(newPassword, serviceName)
            }
        }
}

class VaultListCommand(
    serviceName: String
) : AbstractVaultCommand(
        serviceName = serviceName,
        name = "list",
        help = "List configured OAuth2 credentials",
    ) {
    override fun run() =
        catching {
            if (!vaultProvider.exists()) {
                throw CliktError("Vault does not exist.")
            }
            val vault = openOrCreateVault()
            echo("Env ClientID             Secret")
            echo("=== ==================== ======")
            vault.list().forEach {
                echo("%-3s %-20s ******".format(it.variant, it.name))
            }
        }
}

class VaultClearCommand(
    serviceName: String
) : AbstractVaultCommand(
        serviceName = serviceName,
        name = "clear",
        help = "Clears the credentials of this service",
    ) {
    override fun run() =
        catching {
            if (!vaultProvider.exists()) {
                throw CliktError("Vault does not exist.")
            }
            val vault = openOrCreateVault()
            vault.clear()
        }
}

class VaultStoreCommand(
    serviceName: String
) : AbstractVaultCommand(
        serviceName = serviceName,
        name = "store",
        help = "Store OAuth2 client credentials",
    ) {
    private val env by option(
        "-e",
        "--env",
        help = "Environment. Either tu, ru or pu",
        envvar = "VAULT_ENV",
    ).enum<AdminEnvironment>(ignoreCase = true)
        .prompt("Environment")
    private val clientID by option("-c", "--client-id", help = "OAuth2 ClientID", envvar = "VAULT_CLIENT_ID")
        .prompt("OAuth2 ClientID")
    private val secret by option("-s", "--secret", help = "OAuth2 Client Secret", envvar = "VAULT_CLIENT_SECRET")
        .prompt("OAuth2 Client Secret", hideInput = true)

    override fun run() =
        catching {
            val vault = openOrCreateVault()
            vault.store(env.name, clientID, secret)
        }
}

class VaultExportCommand(
    serviceName: String
) : AbstractVaultCommand(
        serviceName = serviceName,
        name = "export",
        help = "Export Vault to a file for backup or transfer.",
    ) {
    private val output by option("-o", "--output").path(canBeDir = false).required()
    private val transferPassword by option("-t", "--transfer-password")
        .prompt("Enter Vault transfer password", hideInput = true)

    override fun run() =
        catching {
            logger.info { "Exporting Vault to $output" }
            val transferVaultProvider = KeyStoreVaultProvider(customVaultPath = output)
            val transferVault = transferVaultProvider.open(transferPassword, serviceName)
            val vault = openOrCreateVault()

            vault.list().forEach {
                logger.info { "Exporting ${it.variant}:${it.name}" }
                transferVault.store(it.variant, it.name, it.secret)
            }
        }
}

class VaultImportCommand(
    serviceName: String
) : AbstractVaultCommand(
        serviceName = serviceName,
        name = "import",
        help = "Import credentials from another Vault",
    ) {
    private val input by option("-i", "--input").path(canBeDir = false, mustBeReadable = true).required()
    private val transferPassword by option("-t", "--transfer-password")
        .prompt("Enter TRANSFER Vault password", hideInput = true)

    override fun run() =
        catching {
            logger.info { "Importing Vault from $input" }
            val transferVault = KeyStoreVaultProvider(customVaultPath = input).open(transferPassword, serviceName)
            val vault = openOrCreateVault()

            transferVault.list().forEach {
                logger.debug { "Import ${it.variant}:${it.name}" }
                vault.store(it.variant, it.name, it.secret)
            }
        }
}

class VaultPurgeCommand : CliktCommand(name = "purge", help = "Remove Vault") {
    protected val vaultProvider = KeyStoreVaultProvider()

    override fun run() =
        catching {
            if (confirm("Are you sure you want to delete ALL secrets stored in the vault?") == true) {
                vaultProvider.purge()
                echo("Vault purged.")
            } else {
                echo("Abort. Vault left intact.")
            }
        }
}
