package vzd.admin.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.groups.OptionGroup
import com.github.ajalt.clikt.parameters.groups.provideDelegate
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.prompt
import com.github.ajalt.clikt.parameters.types.choice
import mu.KotlinLogging
import vzd.admin.client.KeyStoreVaultProvider

private val logger = KotlinLogging.logger {}

class VaultCommand: CliktCommand(name = "vault", help = "Manage OAuth credentials in the Vault") {
    init {
        subcommands(VaultResetCommand(), VaultListCommand(), VaultStoreCommand())
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
    private val logger = KotlinLogging.logger {}
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
