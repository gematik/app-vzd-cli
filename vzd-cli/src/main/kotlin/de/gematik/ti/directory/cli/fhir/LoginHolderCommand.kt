package de.gematik.ti.directory.cli.fhir

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.requireObject
import de.gematik.ti.directory.DirectoryAuthException
import de.gematik.ti.directory.cli.admin.AdminAPI
import de.gematik.ti.directory.cli.catching
import de.gematik.ti.directory.cli.util.TokenStore
import mu.KotlinLogging

class LoginHolderCommand : CliktCommand(name = "login", help = "Login as Holder (Admin)") {
    private val logger = KotlinLogging.logger {}
    private val context by requireObject<FhirCliEnvironmentContext>()

    override fun run() =
        catching {
            val adminAPI = AdminAPI(context.fhirAPI.globalAPI)
            val env = context.env
            val adminEnvConfig = adminAPI.config.environment(env)
            val tokenStore = TokenStore()
            val tokenEntry =
                tokenStore.accessTokenFor(adminEnvConfig.apiURL) ?: throw DirectoryAuthException("You are not logged in as admin. Use `vzd-cli admin $env login` first.")
            try {
                context.fhirAPI.loginHolder(env, tokenEntry.accessToken)
            } catch (e: DirectoryAuthException) {
                throw DirectoryAuthException("Unable to login into FHIR Directory, re-login as admin required. Use `vzd-cli admin $env login` first.")
            }
            println("Login to `$env` successful (FHIR Holder)")
        }
}
