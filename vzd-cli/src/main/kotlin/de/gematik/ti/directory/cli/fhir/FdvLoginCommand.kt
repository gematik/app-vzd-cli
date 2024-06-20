package de.gematik.ti.directory.cli.fhir

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.CliktError
import com.github.ajalt.clikt.core.requireObject
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.prompt
import de.gematik.ti.directory.cli.catching

class FdvLoginCommand : CliktCommand(name = "login", help = "Login to FDVSearchAPI") {
    private val context by requireObject<FhirCliEnvironmentContext>()
    private val password by option(
        "--password",
        "-p",
        help = "Password for protection of the Vault",
        envvar = "VAULT_PASSWORD",
    ).prompt("Enter Vault Password", hideInput = true)

    override fun run() =
        catching {
            val vault = context.fhirAPI.openVaultFdv(password)
            val credentials = vault.get(context.env.name) ?: throw CliktError("No credentials found for ${context.env.name}")
            context.fhirAPI.loginFdv(context.env, credentials.name, credentials.secret)
            echo("Logged in as ${credentials.name} to FHIR FDV API (${context.env.name})")
        }
}
