package de.gematik.ti.directory.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.multiple
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.prompt
import de.gematik.ti.directory.DirectoryEnvironment
import de.gematik.ti.directory.cli.admin.AdminAPI
import de.gematik.ti.directory.cli.fhir.FhirAPI
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

class LoginCommand : CliktCommand(name = "login", help = "Logins into all configured APIs") {
    private val environments by argument().multiple(default = listOf("pu", "ru", "tu"))
    private val password by option(
        "--password",
        "-p",
        help = "Password for protection of the Vault",
        envvar = "VAULT_PASSWORD",
    ).prompt("Enter Vault Password", hideInput = true)

    override fun run() {
        catching {
            val globalAPI = GlobalAPI()
            val adminAPI = AdminAPI(globalAPI)
            val adminVault = adminAPI.openVault(password)
            adminVault.list().forEach {
                if (environments.contains(it.variant)) {
                    try {
                        adminAPI.login(DirectoryEnvironment.valueOf(it.variant), it.name, it.secret)
                        echo("Logged in as ${it.name} to Admin API (${it.variant})")
                    } catch (e: Exception) {
                        echo("Failed to login as ${it.name} to Admin API (${it.variant})")
                        logger.debug(e) { "Stacktrace of previous error" }
                    }
                }
            }

            val fhirAPI = FhirAPI(globalAPI)
            val fhirVault = fhirAPI.openVaultFdv(password)

            fhirVault.list().forEach {
                if (environments.contains(it.variant)) {
                    try {
                        fhirAPI.loginFdv(DirectoryEnvironment.valueOf(it.variant), it.name, it.secret)
                        echo("Logged in as ${it.name} to FHIR FDV API (${it.variant})")
                    } catch (e: Exception) {
                        echo("Failed to login as ${it.name} to FHIR FDV API (${it.variant})")
                        logger.debug(e) { "Stacktrace of previous error" }
                    }
                }
            }
        }
    }
}
