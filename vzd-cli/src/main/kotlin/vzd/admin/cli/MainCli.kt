package vzd.admin.cli

import com.github.ajalt.clikt.core.*
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.switch
import io.github.cdimascio.dotenv.Dotenv
import io.ktor.client.plugins.auth.providers.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.SerializationException
import mu.KotlinLogging
import vzd.tools.directoryadministration.Client
import vzd.tools.directoryadministration.ClientCredentialsAuthenticator
import vzd.tools.directoryadministration.VZDResponseException

private val logger = KotlinLogging.logger {}

/**
 * Must love Kotlin - create a simple try / catch function and use in all classes that throws these exceptions
 */
fun catching(throwingBlock: () -> Unit = {}) {
    try {
        throwingBlock()
    } catch (e: VZDResponseException) {
        throw CliktError(e.details)
    } catch (e: SerializationException) {
        throw CliktError(e.message)
    }
}

enum class OutputFormat {
    HUMAN, JSON, YAML, CSV, SHORT
}

class CommandContext(
    val client: Client,
    val outputFormat: OutputFormat,
    val syncMode: Boolean=false,
    var firstCommand: Boolean = true,
)

class DirectoryAdministrationCli : CliktCommand(name="admin", allowMultipleSubcommands = true, help="""CLI for DirectoryAdministration API

Commands require following environment variables:
 
```
 - ADMIN_API_URL
 - either ADMIN_AUTH_URL, ADMIN_CLIENT_ID, ADMIN_CLIENT_SECRET
 -     or ADMIN_ACCESS_TOKEN 
 - Optional: HTTP_PROXY_URL
``` 
""".trimMargin()) {
    private val dotenv by requireObject<Dotenv>()
    private val outputFormat by option().switch(
        "--human" to OutputFormat.HUMAN,
        "--json" to OutputFormat.JSON,
        "--yaml" to OutputFormat.YAML,
        "--csv" to OutputFormat.CSV,
        "--short" to OutputFormat.SHORT,
    ).default(OutputFormat.HUMAN)

    private val sync by option(help="use Sync mode").flag()
    override fun run() = catching {

        val client = Client {
            dotenv.get("ADMIN_API_URL", null) ?: throw UsageError("Environment variable ADMIN_API_URL is not set")

            apiURL = dotenv["ADMIN_API_URL"]
            val accessToken = dotenv.get("ADMIN_ACCESS_TOKEN", null)
            if (accessToken != null) {
                logger.debug { "Found ADMIN_ACCESS_TOKEN env variable. Using it to authenticate. " }
                loadTokens = {
                    BearerTokens(accessToken, "")
                }
            } else {
                dotenv.get("ADMIN_AUTH_URL", null) ?: throw UsageError("Environment variable ADMIN_AUTH_URL is not set")
                dotenv.get("ADMIN_CLIENT_ID", null) ?: throw UsageError("Environment variable ADMIN_CLIENT_ID is not set")
                dotenv.get("ADMIN_CLIENT_SECRET", null) ?: throw UsageError("Environment variable ADMIN_CLIENT_SECRET is not set")
                loadTokens = {
                    val auth = ClientCredentialsAuthenticator(dotenv["ADMIN_AUTH_URL"], dotenv.get("HTTP_PROXY_URL", null))
                    auth.authenticate(dotenv["ADMIN_CLIENT_ID"], dotenv["ADMIN_CLIENT_SECRET"])
                }
            }

            httpProxyURL = dotenv.get("HTTP_PROXY_URL", null)
        }

        currentContext.obj = CommandContext(client, outputFormat, syncMode = sync)
    }
    init {
        subcommands(Info(), AuthenticateAdmin(), ListCommand(), TempolateCommand(), AddBaseCommand(),
            LoadBaseCommand(), ModifyBaseDirectoryEntry(), ModifyBaseAttrCommand(), DeleteCommand(),
            ListCertificates(), AddCertificate(), DeleteCertificates())
    }

}

class AuthenticateAdmin: CliktCommand(name="auth", help="Perform authentication") {
    private val dotenv by requireObject<Dotenv>()
    override fun run() = catching {
        logger.debug { "Executing command: AuthenticateAdmin" }
        val auth = ClientCredentialsAuthenticator(dotenv["ADMIN_AUTH_URL"], dotenv.get("HTTP_PROXY_URL", null))
        val tokens = auth.authenticate(dotenv["ADMIN_CLIENT_ID"], dotenv["ADMIN_CLIENT_SECRET"])
        println (tokens.accessToken)
    }
}

class Info: CliktCommand(name="info", help="Show information about the API") {
    private val context by requireObject<CommandContext>()

    override fun run() = catching {
        val info = runBlocking { context.client.getInfo() }
        when (context.outputFormat) {
            OutputFormat.JSON -> Output.printJson(info)
            OutputFormat.HUMAN, OutputFormat.YAML -> Output.printYaml(info)
            else -> throw UsageError("Info is not available for format: ${context.outputFormat}")
        }
    }

}

