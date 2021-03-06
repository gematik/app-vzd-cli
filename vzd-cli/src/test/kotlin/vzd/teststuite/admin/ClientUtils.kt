package vzd.teststuite.admin

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import io.github.cdimascio.dotenv.dotenv
import org.slf4j.LoggerFactory
import vzd.admin.client.Client
import vzd.admin.client.FileConfigProvider

fun createClient(): Client {
    var dotenv = dotenv { ignoreIfMissing = true }
    val root: Logger = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME) as Logger
    root.level = Level.DEBUG

    val provider = FileConfigProvider()
    val tucfg = provider.config.environment("tu") ?: throw IllegalStateException()

    return Client {
        apiURL = tucfg.apiURL
        accessToken = dotenv.get("TEST_ACCESS_TOKEN") ?: throw RuntimeException("Environment variable 'TEST_ACCESS_TOKEN' must be set.")
        if (provider.config.httpProxy?.enabled == true) {
            httpProxyURL = provider.config.httpProxy?.proxyURL
        }
    }
}
