package vzd.tools.directoryadministration.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.requireObject
import com.github.ajalt.clikt.core.subcommands
import io.github.cdimascio.dotenv.Dotenv
import io.ktor.client.plugins.auth.providers.*
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import net.mamoe.yamlkt.Yaml
import vzd.tools.directoryadministration.Client
import vzd.tools.directoryadministration.ClientCredentialsAuthenticator

private val logger = KotlinLogging.logger {}

class DirectoryAdministrationCli : CliktCommand(name="admin", help="""CLI for DirectoryAdministration API

Commands require following environment variables:
 
```
 - ADMIN_API_URL
 - ADMIN_AUTH_URL, ADMIN_CLIENT_ID, ADMIN_CLIENT_SECRET
 - ... or ADMIN_ACCESS_TOKEN 
``` 
""".trimMargin()) {
    private val dotenv by requireObject<Dotenv>()
    override fun run() {

        currentContext.obj = Client {
            apiURL = dotenv["ADMIN_API_URL"]
            val accessToken = dotenv.get("ADMIN_ACCESS_TOKEN", null)
            if (accessToken != null) {
                logger.debug { "Found ADMIN_ACCESS_TOKEN env variable. Using it to authenticate. " }
                loadTokens = {
                    BearerTokens(accessToken, "")
                }
            } else {
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
    override fun run() {
        logger.debug { "Executing command: AuthenticateAdmin" }
        val auth = ClientCredentialsAuthenticator(dotenv["ADMIN_AUTH_URL"])
        val tokens = auth.authenticate(dotenv["ADMIN_CLIENT_ID"], dotenv["ADMIN_CLIENT_SECRET"])
        println (tokens.accessToken)
    }
}

class Info: CliktCommand(name="info", help="Show information about the API") {
    private val client by requireObject<Client>()
    override fun run() {
        val info = runBlocking { client.getInfo() }
        println(Yaml{}.encodeToString(info))
    }
}