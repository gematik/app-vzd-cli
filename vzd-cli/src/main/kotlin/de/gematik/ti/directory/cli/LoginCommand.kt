package de.gematik.ti.directory.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.multiple
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.prompt
import de.gematik.ti.directory.DirectoryAuthException
import de.gematik.ti.directory.DirectoryEnvironment
import de.gematik.ti.directory.cli.admin.AdminAPI
import de.gematik.ti.directory.cli.fhir.FhirAPI
import de.gematik.ti.directory.cli.util.TokenStore
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
            val fhirAPI = FhirAPI(globalAPI)

            val adminVault = adminAPI.openVault(password)
            adminVault.list().forEach {
                if (environments.contains(it.variant)) {
                    val env = DirectoryEnvironment.valueOf(it.variant)
                    try {
                        adminAPI.login(env, it.name, it.secret)
                        echo("Logged in as ${it.name} to Admin API (${it.variant})")
                    } catch (e: Exception) {
                        echo("Failed to login as ${it.name} to Admin API (${it.variant})")
                        logger.debug(e) { "Stacktrace of previous error" }
                    }
                    try {
                        doLoginFhirHolder(adminAPI, fhirAPI, env)
                        echo("Logged in as ${it.name} to FHIR Holder API (${it.variant})")
                    } catch (e: Exception) {
                        echo("Failed to login as ${it.name} to FHIR Holder API (${it.variant})")
                        logger.debug(e) { "Stacktrace of previous error" }
                    }
                }
            }

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

fun doLoginFhirHolder(
    adminAPI: AdminAPI,
    fhirAPI: FhirAPI,
    env: DirectoryEnvironment
) {
    val envConfig = adminAPI.config.environment(env)
    val tokenStore = TokenStore()
    val tokenEntry =
        tokenStore.accessTokenFor(envConfig.apiURL) ?: throw DirectoryAuthException("Fatal error")
    fhirAPI.loginHolder(env, tokenEntry.accessToken)
}
