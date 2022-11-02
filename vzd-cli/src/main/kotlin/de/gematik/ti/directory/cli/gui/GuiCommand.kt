package de.gematik.ti.directory.cli.gui

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.output.TermUi
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.int
import de.gematik.ti.directory.bff.directoryModule
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import kotlinx.coroutines.launch
import java.net.URL

class GuiCommand : CliktCommand(name = "gui", help = """Starts HTTP Server with GUI""".trimMargin()) {
    val port by option("-p", "--port").int().default(57036)

    override fun run() {
        embeddedServer(Netty, port = port, host = "127.0.0.1") {
            directoryModule()

            environment.monitor.subscribe(ApplicationStarted) {
                TermUi.echo("Starting server at: http://127.0.0.1:$port")
                val url = "http://127.0.0.1:$port/"
                val os = System.getProperty("os.name").lowercase()
                launch {
                    repeat(10) {
                        Thread.sleep(250)
                        try {
                            URL(url).openConnection()
                            if (os.contains("win")) {
                                Runtime.getRuntime().exec(arrayOf("start", url))
                            } else if (os.contains("mac")) {
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
    }
}
