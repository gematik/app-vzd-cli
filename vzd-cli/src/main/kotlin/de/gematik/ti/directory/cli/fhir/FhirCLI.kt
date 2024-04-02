package de.gematik.ti.directory.cli.fhir


import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.CliktError
import com.github.ajalt.clikt.core.requireObject
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.help
import com.github.ajalt.clikt.parameters.arguments.multiple
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.multiple
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.switch
import com.github.ajalt.clikt.parameters.types.enum
import de.gematik.ti.directory.DirectoryEnvironment
import de.gematik.ti.directory.cli.GlobalAPI
import de.gematik.ti.directory.cli.catching
import de.gematik.ti.directory.fhir.SearchQuery
import de.gematik.ti.directory.fhir.SearchResource
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

enum class ResourceName(val searchResource: SearchResource) {
    PractitionerRole(SearchResource.PractitionerRole),
    HealthcareService(SearchResource.HealthcareService),
    PR(SearchResource.PractitionerRole),
    HS(SearchResource.HealthcareService),
}

class SearchCommand: CliktCommand(name = "search", help = "Search FHIR Directory using SearchAPI") {
    private val logger = KotlinLogging.logger {}
    private val outputFormat by option().switch(
        "--json" to OutputFormat.JSON,
        "--human" to OutputFormat.HUMAN,
    ).default(OutputFormat.HUMAN)

    private val include by option("--include", "-i", help = "Include referenced resources").multiple()

    private val resource by argument(help = "Resource type to search for").enum<ResourceName>(ignoreCase = true)
        .help(ResourceName.entries.joinToString(", ") { it.name })

    private val params: List<String> by argument(help = "Query parameters").multiple().help("key=value")

    private val context by requireObject<FhirCliEnvironmentContext>()

    fun parseParams(): SearchQuery {
        val query = SearchQuery()
        params.forEach {
            if (!it.contains("=")) {
                throw CliktError("Invalid parameter: $it")
            }
            val (key, value) = it.split("=")
            if (query.params.containsKey(key)) {
                query.params[key] = query.params[key]!!.plus(value)
            } else {
                query.params[key] = listOf(value)
            }
        }
        include.forEach {
            query.params["_include"] = query.params["_include"]?.plus(it) ?: listOf(it)
        }
        return query
    }

    override fun run() =
        catching {
            logger.info { "Searching FHIR Directory ${context.env.name}" }
            var query = parseParams()
            runBlocking {
                val bundle = context.client.search(resource.searchResource, query)
                echo(bundle.toStringOutput(outputFormat))
            }
        }
}
