package de.gematik.ti.directory.cli.gui

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.int
import de.gematik.ti.directory.admin.Client
import de.gematik.ti.directory.admin.FileConfigProvider
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.locations.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.json.Json

class GuiCommand : CliktCommand(name = "gui", help = """Starts HTTP Server with GUI""".trimMargin()) {
    val port by option("-p", "--port").int().default(57036)

    override fun run() {
        embeddedServer(Netty, port = port, host = "0.0.0.0") {
            install(Locations)
            install(ContentNegotiation) {
                json(Json {
                    prettyPrint = true
                    isLenient = true
                })
            }
            configureRouting()
            configureSearchRouting()
        }.start(wait = true)
        echo("Listening server at: $port")

    }

}




@Location("/{env}/search")
data class Search(val env: String, val q: String)

fun downstreamClient(env: String): Client {
    val config = FileConfigProvider().config
    val envConfig = config.environment(env)
    val client = Client() {
        apiURL = envConfig!!.apiURL
        accessToken = config.tokens?.get(env)?.accessToken!!
        if (config.httpProxy.enabled) {
            httpProxyURL = config.httpProxy.proxyURL
        }
    }
    return client
}

fun Application.configureSearchRouting() {
    routing() {
        route("api") {
            route("admin") {
                get<Search> { search ->
                    val client = downstreamClient(search.env)
                    call.respond(client.search(search.q))
                }
                get("{env}/DirectoryEntry/{telematikID}") {
                    val client = downstreamClient(call.parameters.get("env")!!)
                    val entries = client.readDirectoryEntry(mapOf("telematikID" to call.parameters.get("telematikID")!!))

                    entries?.first()?.also {
                        call.respond(it)
                    }
                }
            }
        }
    }
}

fun Application.configureRouting() {
    routing () {
        route("api") {
            get("config") {
                val provider = FileConfigProvider()
                provider.config.tokens = null
                call.respond(provider.config)
            }
        }
    }
}