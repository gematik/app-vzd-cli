package de.gematik.ti.directory.teststuite.admin

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import de.gematik.ti.directory.admin.AdminAPI
import de.gematik.ti.directory.admin.AdminEnvironment
import de.gematik.ti.directory.admin.Client
import de.gematik.ti.directory.global.GlobalAPI
import org.slf4j.LoggerFactory

fun createClient(): Client {
    val root: Logger = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME) as Logger
    root.level = Level.DEBUG

    val adminAPI = AdminAPI(GlobalAPI())
    val tucfg = adminAPI.config.environment(AdminEnvironment.tu)

    return Client {
        apiURL = tucfg.apiURL
        accessToken = System.getenv()["TEST_ACCESS_TOKEN"] ?: throw RuntimeException("Environment variable 'TEST_ACCESS_TOKEN' must be set.")
        if (adminAPI.globalAPI.config.httpProxy.enabled) {
            httpProxyURL = adminAPI.globalAPI.config.httpProxy.proxyURL
        }
    }
}
