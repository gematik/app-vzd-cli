package vzd.tools.directoryadministration.cli

import com.github.ajalt.clikt.core.*
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import io.github.cdimascio.dotenv.Dotenv
import io.ktor.client.plugins.auth.providers.*
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import net.mamoe.yamlkt.Yaml
import vzd.tools.directoryadministration.*

private val logger = KotlinLogging.logger {}

/**
 * Must love Kotlin - create a simple try / catch function and use in all classes that thriws these exceptions
 */
fun catching(throwingBlock: () -> Unit = {}) {
    try {
        throwingBlock()
    } catch (e: VZDResponseException) {
        throw CliktError(e.details)
    }
}

class DirectoryAdministrationCli : CliktCommand(name="admin", help="""CLI for DirectoryAdministration API

Commands require following environment variables:
 
```
 - ADMIN_API_URL
 - ADMIN_AUTH_URL, ADMIN_CLIENT_ID, ADMIN_CLIENT_SECRET
 - ... or ADMIN_ACCESS_TOKEN 
``` 
""".trimMargin()) {
    private val dotenv by requireObject<Dotenv>()
    override fun run() = catching {

        currentContext.obj = Client {
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
                    val auth = ClientCredentialsAuthenticator(dotenv["ADMIN_AUTH_URL"])
                    auth.authenticate(dotenv["ADMIN_CLIENT_ID"], dotenv["ADMIN_CLIENT_SECRET"])
                }
            }
        }
    }
    init {
        subcommands(Info(), AuthenticateAdmin(), ListDirectoryEntries(), AddDirectoryEntry(), LoadBaseDirectoryEntry(),
            ModifyBaseDirectoryEntry(), DeleteDiectoryEntry(), ListCertificates(), AddCertificate(), DeleteCertificates())
    }

}

class AuthenticateAdmin: CliktCommand(name="auth", help="Perform authentication") {
    private val dotenv by requireObject<Dotenv>()
    override fun run() = catching {
        logger.debug { "Executing command: AuthenticateAdmin" }
        val auth = ClientCredentialsAuthenticator(dotenv["ADMIN_AUTH_URL"])
        val tokens = auth.authenticate(dotenv["ADMIN_CLIENT_ID"], dotenv["ADMIN_CLIENT_SECRET"])
        println (tokens.accessToken)
    }
}


class Info: CliktCommand(name="info", help="Show information about the API") {
    private val verbose by option("--verbose").required()
    private val client by requireObject<Client>()

    override fun run() = catching {
        val info = runBlocking { client.getInfo() }
        println(Yaml{}.encodeToString(info))
    }

}

