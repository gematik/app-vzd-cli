package de.gematik.ti.directory.cli.gui

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.CliktError
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.int
import de.gematik.ti.directory.bff.directoryModule
import de.gematik.ti.directory.cli.catching
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import kotlinx.coroutines.launch
import mu.KotlinLogging
import java.net.BindException
import java.net.URL

private val logger = KotlinLogging.logger {}

class GuiCommand : CliktCommand(name = "gui", help = """Starts HTTP Server with GUI""".trimMargin()) {
    val port by option("-p", "--port").int().default(57036)

    override fun run() = catching {
        val url = "http://127.0.0.1:$port/"
        println("Starting server at: $url")

        val os = System.getProperty("os.name").lowercase()

        try {
            embeddedServer(Netty, port = port, host = "127.0.0.1") {
                directoryModule()

                environment.monitor.subscribe(ApplicationStarted) {
                    launch {
                        repeat(10) {
                            Thread.sleep(250)
                            try {
                                logger.debug { "Trying to connect to $url" }
                                URL(url).openConnection().getInputStream().readBytes()
                                logger.debug { "URL connection successful. Opening the Webbrowser." }
                                if (os.contains("win")) {
                                    logger.debug { "Opening browser on windows." }
                                    try {
                                        Runtime.getRuntime().exec(arrayOf("rundll32", "url.dll,FileProtocolHandler", url))
                                    } catch (e: Throwable) {
                                        throw CliktError(e.toString())
                                    }
                                } else if (os.contains("mac")) {
                                    logger.debug { "Opening browser on a mac." }
                                    Runtime.getRuntime().exec(arrayOf("open", url))
                                } else {
                                    echo("Open the following URL in your web browser: $url")
                                }
                                return@launch
                            } catch (e: Throwable) {
                                // retry
                            }
                        }
                    }
                }
            }.start(wait = true)
        } catch (e: BindException) {
            echo("Server is already running at $url")
        }
    }
}
