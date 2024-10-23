package de.gematik.ti.directory.cli.fhir

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.requireObject
import com.github.ajalt.clikt.parameters.options.option
import de.gematik.ti.directory.cli.catching

class FdvTokenCommand : CliktCommand(name = "token", help = "Set or get Access Token for FDVSearchAPI") {
    private val context by requireObject<FhirCliEnvironmentContext>()
    private val token by option(
        "-s",
        "--set",
        metavar = "ACCESS_TOKEN",
        help = "Sets OAuth2 Access Token",
        envvar = "FDV_SEARCH_ACCESS_TOKEN",
    )

    override fun run() =
        catching {
            token?.let { context.fhirAPI.storeAccessTokenFdv(context.env, it) }

            echo(
                context.fhirAPI.retrieveAccessTokenFdv(context.env),
            )
        }
}
