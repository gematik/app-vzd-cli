package de.gematik.ti.directory.cli.bff

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.context
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.help
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.enum
import com.github.ajalt.clikt.parameters.types.int
import de.gematik.ti.directory.DirectoryEnvironment
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.http.content.*
import io.ktor.server.netty.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.slf4j.LoggerFactory
import kotlin.time.Duration.Companion.seconds

enum class LogLevelOption { ERROR, WARN, INFO, DEBUG }

class BffStartCommand : CliktCommand(name = "start", help = "Start the client as backend for frontend") {
    init {
        context { autoEnvvarPrefix = "DIRECTORY" }
    }

    val port by option().int().default(57036)
    val urlPath by option().default("")
    val logLevel by option().enum<LogLevelOption>().default(LogLevelOption.INFO)
    val expiresIn by option().int().default(60 * 60 * 4).help("Duration in seconds when the access token is considered expired and must be refreshed. Default 4 Hours (14400 seconds)")
    val adminRuClientId by option().default("")
    val adminRuClientSecret by option().default("")
    val adminTuClientId by option().default("")
    val adminTuClientSecret by option().default("")
    val adminPuClientId by option().default("")
    val adminPuClientSecret by option().default("")
    val httpProxyUrl by option().default("")

    override fun run() {
        val root: Logger = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME) as Logger
        when (logLevel) {
            LogLevelOption.ERROR -> root.level = Level.ERROR
            LogLevelOption.WARN -> root.level = Level.WARN
            LogLevelOption.INFO -> root.level = Level.INFO
            LogLevelOption.DEBUG -> root.level = Level.DEBUG
        }

        val tokenManager =
            TokenManager(
                defaultExpiresIn = expiresIn.seconds,
            )

        if (urlPath != "") {
            throw IllegalArgumentException("URL path is not supported yet")
        }

        val prefix = urlPath.removeSuffix("/") + "/"

        val server =
            embeddedServer(Netty, port = port) {
                directoryModule {
                    if (httpProxyUrl != "") {
                        globalAPI.config.httpProxy.enabled = true
                        globalAPI.config.httpProxy.proxyURL = httpProxyUrl
                        tokenManager.httpProxyUrl = httpProxyUrl
                    }
                    adminAPI.tokenProvider = { apiURL ->
                        tokenManager.accessTokenFor(apiURL)
                    }
                    routing = {
                        route("${prefix}api") {
                            logger.info { "Configuring API routes: ${prefix}api" }
                            adminRoutes()
                            route("{...}") {
                                handle {
                                    call.respond(HttpStatusCode.NotFound)
                                }
                            }
                        }
                        logger.info { "Configuring SPA frontend: ${prefix}index.html" }
                        singlePageApplication {
                            applicationRoute = prefix
                            useResources = true
                            filesPath = "directory-app/browser"
                            defaultPage = "index.html"
                        }
                    }
                }
            }
        // TODO proxy
        tokenManager.httpProxyUrl = httpProxyUrl

        if (adminTuClientId != "") {
            tokenManager.registerAdminCredentials(DirectoryEnvironment.tu, adminTuClientId, adminTuClientSecret)
        }

        if (adminRuClientId != "") {
            tokenManager.registerAdminCredentials(DirectoryEnvironment.ru, adminRuClientId, adminRuClientSecret)
        }

        if (adminPuClientId != "") {
            tokenManager.registerAdminCredentials(DirectoryEnvironment.pu, adminPuClientId, adminPuClientSecret)
        }

        server.start(wait = true)
    }
}
