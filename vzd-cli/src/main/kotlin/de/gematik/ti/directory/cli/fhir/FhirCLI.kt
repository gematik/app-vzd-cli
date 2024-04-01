package de.gematik.ti.directory.cli.fhir


import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.requireObject
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.options.option
import de.gematik.ti.directory.DirectoryEnvironment
import de.gematik.ti.directory.cli.GlobalAPI
import de.gematik.ti.directory.cli.VaultCommand
import de.gematik.ti.directory.cli.catching
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging

val logger = KotlinLogging.logger {}

class FhirCliContext(
    val fhirAPI: FhirAPI,
)

class FhirCliEnvironmentContext(
    val fhirAPI: FhirAPI,
    var env: DirectoryEnvironment,
) {
    val client by lazy {
        fhirAPI.createClient(env)
    }

}

class FhirCli :
    CliktCommand(name = "fhir", help = """CLI for FHIR Directory""".trimMargin()) {
    override fun run() =
        catching {
            val adminAPI = FhirAPI(GlobalAPI())
            currentContext.obj = FhirCliContext(adminAPI)
        }

    init {
        subcommands(
            EnvironmentCommands(DirectoryEnvironment.pu),
            EnvironmentCommands(DirectoryEnvironment.ru),
            EnvironmentCommands(DirectoryEnvironment.tu),
        )
    }
}

class EnvironmentCommands(env: DirectoryEnvironment) : CliktCommand(name = env.name, help = """Commands for $env instance""".trimMargin()) {
    private val context by requireObject<FhirCliContext>()

    init {
        subcommands(
            FdvCommands(),
            SearchTokenCommand(),
            SearchCommand()
        )
    }

    override fun run() =
        catching {
            currentContext.obj = FhirCliEnvironmentContext(context.fhirAPI, DirectoryEnvironment.valueOf(commandName))
        }
}

class SearchTokenCommand: CliktCommand(name = "token", help = "Set or get Access Token for SearchAPI") {
    private val context by requireObject<FhirCliEnvironmentContext>()
    private val token by option("-s", "--set", metavar = "ACCESS_TOKEN", help = "Sets OAuth2 Access Token", envvar = "SEARCH_ACCESS_TOKEN")

    override fun run() =
        catching {

            token?.let { context.fhirAPI.storeAccessTokenSearch(context.env, it) }

            echo(
                context.fhirAPI.retrieveAccessTokenSearch(context.env)
            )
        }
}

class SearchCommand: CliktCommand(name = "search", help = "Search FHIR Directory using SearchAPI") {
    private val logger = KotlinLogging.logger {}

    private val context by requireObject<FhirCliEnvironmentContext>()

    override fun run() =
        catching {
            logger.info { "Searching FHIR Directory ${context.env.name}" }
            runBlocking {
                val bundle = context.client.search("customParams, parameterOptions")
                echo(bundle.toPrettyJson())
            }
        }
}
