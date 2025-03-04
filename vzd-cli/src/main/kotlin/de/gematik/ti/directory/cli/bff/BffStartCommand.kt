package de.gematik.ti.directory.cli.bff

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.UsageError
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
    val baseHref by option().default("")
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

        val tokenProvider =
            ClientCredentialsTokenProvider(
                defaultExpiresIn = expiresIn.seconds,
            )

        if (baseHref != "" && !baseHref.matches(Regex("^/[^/]+$"))) {
            throw UsageError("Invalid path prefix: $baseHref. Prefix must start with / and contain only one slash.")
        }

        var indexHtml =
            BffStartCommand::class.java.getResource("/directory-app/browser/index.html")?.readText()
                ?: throw IllegalStateException("Could not find index.html in resources")

        indexHtml = indexHtml.replace("<base href=\"/\">", "<base href=\"$baseHref/\">")
        indexHtml = indexHtml.replace("window.__baseHref = '/'", "window.__baseHref = '$baseHref/'")

        val server =
            embeddedServer(Netty, port = port) {
                directoryModule {
                    if (httpProxyUrl != "") {
                        globalAPI.config.httpProxy.enabled = true
                        globalAPI.config.httpProxy.proxyURL = httpProxyUrl
                        tokenProvider.httpProxyUrl = httpProxyUrl
                    }
                    adminAPI.tokenProvider = tokenProvider
                    routing = {
                        route("$baseHref/api") {
                            logger.info { "Configuring API routes: $baseHref/api" }
                            adminRoutes()
                            route("{...}") {
                                handle {
                                    call.respond(HttpStatusCode.NotFound)
                                }
                            }
                        }
                        logger.info { "Configuring SPA frontend: $baseHref/index.html" }
                        route("$baseHref/{path...}") {
                            handle {
                                val paths = call.parameters.getAll("path").orEmpty()
                                val content = call.resolveResource(paths.joinToString("/"), "directory-app/browser")
                                if (content != null) {
                                    call.respond(content)
                                } else {
                                    call.respondText(indexHtml, ContentType.Text.Html)
                                }
                            }
                        }
                        route("$baseHref/") {
                            handle {
                                call.respondText(indexHtml, ContentType.Text.Html)
                            }
                        }
                    }
                }
            }
        if (httpProxyUrl != "") {
            tokenProvider.httpProxyUrl = httpProxyUrl
        }

        if (adminTuClientId != "") {
            tokenProvider.registerAdminCredentials(DirectoryEnvironment.tu, adminTuClientId, adminTuClientSecret)
        }

        if (adminRuClientId != "") {
            tokenProvider.registerAdminCredentials(DirectoryEnvironment.ru, adminRuClientId, adminRuClientSecret)
        }

        if (adminPuClientId != "") {
            tokenProvider.registerAdminCredentials(DirectoryEnvironment.pu, adminPuClientId, adminPuClientSecret)
        }

        server.start(wait = true)
    }
}
