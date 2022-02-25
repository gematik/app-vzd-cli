package vzd.teststuite.admin

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import com.github.ajalt.clikt.core.UsageError
import io.github.cdimascio.dotenv.dotenv
import io.ktor.client.plugins.auth.providers.*
import org.slf4j.LoggerFactory
import vzd.admin.client.Client
import vzd.admin.client.ClientCredentialsAuthenticator


fun createClient(): Client {
    var dotenv = dotenv { ignoreIfMissing = true }
    val root: Logger = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME) as Logger
    root.level = Level.DEBUG

    return Client {
        apiURL = dotenv["ADMIN_API_URL"]
        dotenv.get("ADMIN_ACCESS_TOKEN", null)?.let {
            loadTokens = { BearerTokens(it, "") }
        } ?: run {
            dotenv.get("ADMIN_AUTH_URL", null) ?: throw UsageError("Environment variable ADMIN_AUTH_URL is not set")
            dotenv.get("ADMIN_CLIENT_ID", null) ?: throw UsageError("Environment variable ADMIN_CLIENT_ID is not set")
            dotenv.get("ADMIN_CLIENT_SECRET", null)
                ?: throw UsageError("Environment variable ADMIN_CLIENT_SECRET is not set")
            loadTokens = {
                val auth = ClientCredentialsAuthenticator(dotenv["ADMIN_AUTH_URL"], dotenv.get("HTTP_PROXY_URL", null))
                auth.authenticate(dotenv["ADMIN_CLIENT_ID"], dotenv["ADMIN_CLIENT_SECRET"])
            }
        }
    }

}