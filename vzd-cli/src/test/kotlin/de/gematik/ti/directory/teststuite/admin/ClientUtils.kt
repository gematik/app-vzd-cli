package de.gematik.ti.directory.teststuite.admin

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import de.gematik.ti.directory.admin.Client
import de.gematik.ti.directory.admin.FileConfigProvider
import org.slf4j.LoggerFactory

fun createClient(): Client {
    val root: Logger = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME) as Logger
    root.level = Level.DEBUG

    val provider = FileConfigProvider()
    val tucfg = provider.config.environment("tu") ?: throw IllegalStateException()

    return Client {
        apiURL = tucfg.apiURL
        accessToken = System.getenv()["TEST_ACCESS_TOKEN"] ?: throw RuntimeException("Environment variable 'TEST_ACCESS_TOKEN' must be set.")
        if (provider.config.httpProxy.enabled == true) {
            httpProxyURL = provider.config.httpProxy.proxyURL
        }
    }
}
