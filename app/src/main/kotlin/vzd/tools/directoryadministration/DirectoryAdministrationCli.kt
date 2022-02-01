package vzd.tools.directoryadministration

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.UsageError
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.multiple
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.choice
import io.github.cdimascio.dotenv.dotenv
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import mu.KotlinLogging
import net.mamoe.yamlkt.Yaml
import vzd.tools.dotenv

private val logger = KotlinLogging.logger {}

val DirectoryEntryPrinters = mapOf(
    "short" to { value: DirectoryEntry ->
        "${value.directoryEntryBase.dn.uid} ${value.directoryEntryBase.telematikID} ${Json.encodeToString(value.directoryEntryBase.displayName)}"
    },
    "yaml" to { value: DirectoryEntry ->
        Yaml.encodeToString(listOf(value))
    },
    "json" to { value: DirectoryEntry ->
        Json { prettyPrint = true }.encodeToString(value)
    }
)

class ListDirectoryEntries: CliktCommand(name = "list", help="List directory entries") {
    private val parameters by argument(name="KEY=VALUE").multiple(required = true)
    private val sync by option(help="use Sync mode").flag()
    private val printer by option(help="How the entries should be displayed").choice(*DirectoryEntryPrinters.keys.toTypedArray()).default("short")
    override fun run() {
        val client = Client {
            apiURL = dotenv["ADMIN_API_URL"]
            loadTokens = {
                val auth = ClientCredentialsAuthenticator(dotenv["ADMIN_AUTH_URL"])
                auth.authenticate(dotenv["ADMIN_CLIENT_ID"], dotenv["ADMIN_CLIENT_SECRET"])
            }
        }

        val paramsMap = parameters.map {
            val kv = it.split("=")
            if (kv.size != 2) {
                throw UsageError("Bad key/value pair: $it")
            }

            Pair(kv[0], kv[1] )
        }.associateBy ( { it.first }, { it.second } )

        val result: List<DirectoryEntry>?
        if (sync) {
            result = runBlocking {  client.readDirectoryEntryForSync( paramsMap ) }
        } else {
            result = runBlocking {  client.readDirectoryEntry( paramsMap ) }
        }

        result?.forEach {
            println(DirectoryEntryPrinters[printer]?.invoke(it) )
        }

    }

}


class AuthenticateAdmin: CliktCommand(name="auth", help="Perform authentication") {
    override fun run() {
        logger.debug { "Executing command: Auth" }
        val auth = ClientCredentialsAuthenticator(dotenv["ADMIN_AUTH_URL"])
        val tokens = auth.authenticate(dotenv["ADMIN_CLIENT_ID"], dotenv["ADMIN_CLIENT_SECRET"])
        println (tokens.accessToken)
    }
}

class DirectoryAdministrationCli : CliktCommand(name="admin", help="CLI for DirectoryAdministration API") {
    override fun run() = Unit
    init {
        subcommands(AuthenticateAdmin(), ListDirectoryEntries())
    }
}