package de.gematik.ti.directory.cli.fhir

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.CliktError
import com.github.ajalt.clikt.core.requireObject
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.groups.provideDelegate
import com.github.ajalt.clikt.parameters.options.associate
import com.github.ajalt.clikt.parameters.options.option
import de.gematik.ti.directory.cli.VaultCommand
import de.gematik.ti.directory.cli.admin.ParameterOptions
import de.gematik.ti.directory.cli.catching
import de.gematik.ti.directory.cli.util.TokenStore
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging

class FdvCommands : CliktCommand(name = "fdv", help = "Commands for FDVSearchAPI") {
    init {
        subcommands(
            VaultCommand(FDV_SEARCH_SERVICE_NAME, "vault", "Manage FDVSearchAPI credentials in the Vault"),
            FdvTokenCommand(),
            FdvSearchCommand(),
        )
    }

    override fun run() {
        // no-op
    }
}

class FdvTokenCommand: CliktCommand(name = "token", help = "Set or get Access Token for FDVSearchAPI") {
    private val context by requireObject<FhirCliEnvironmentContext>()
    private val token by option("-s", "--set", metavar = "ACCESS_TOKEN", help = "Sets OAuth2 Access Token", envvar = "FDV_SEARCH_ACCESS_TOKEN")

    override fun run() =
        catching {

            token?.let { context.fhirAPI.storeAccessTokenFdv(context.env, it) }

            echo(
                context.fhirAPI.retrieveAccessTokenFdv(context.env)
            )
        }
}

class FdvSearchCommand: CliktCommand(name = "search", help = "Search FHIR Directory using FDVSearchAPI") {
    private val logger = KotlinLogging.logger {}
    private val customParams: Map<String, String> by option(
        "-p",
        "--param",
        help = "Specify query parameters to find matching entries",
    ).associate()
    private val parameterOptions by ParameterOptions()

    // val force by option(help="Force delete").flag()
    private val context by requireObject<FhirCliEnvironmentContext>()

    override fun run() =
        catching {
            logger.info { "Searching FHIR Directory ${context.env.name}" }
            runBlocking {
                context.client.fdvSearch("customParams, parameterOptions")
            }
        }
}