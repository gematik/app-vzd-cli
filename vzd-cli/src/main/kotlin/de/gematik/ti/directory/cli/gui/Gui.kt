package de.gematik.ti.directory.cli.gui

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.int
import de.gematik.ti.directory.admin.Client
import de.gematik.ti.directory.admin.FileConfigProvider
import de.gematik.ti.directory.admin.quickSearch
import io.ktor.http.*
import io.ktor.http.parsing.*
import io.ktor.resources.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.resources.*
import io.ktor.server.resources.Resources
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class GuiCommand : CliktCommand(name = "gui", help = """Starts HTTP Server with GUI""".trimMargin()) {
    val port by option("-p", "--port").int().default(57036)

    override fun run() {
        embeddedServer(Netty, port = port, host = "0.0.0.0") {
            install(Resources)
            install(ContentNegotiation) {
                json(
                    Json {
                        prettyPrint = true
                        isLenient = true
                    }
                )
            }
            configureSearchRouting()

            install(StatusPages) {
                exception<ParseException> { call, cause ->
                    call.respondText(text = "401: Unauthorized (token expired)" , status = HttpStatusCode.Unauthorized)
                }
            }
        }.start(wait = true)
        echo("Listening server at: $port")
    }
}
@Resource("/{env}/search")
@Serializable
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
    routing {  }() {
        route("api") {
            route("admin") {
                get<Search> { search ->
                    val client = downstreamClient(search.env)
                    call.respond(client.quickSearch(search.q))
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
    routing() {
        route("api") {
            get("config") {
                val provider = FileConfigProvider()
                provider.config.tokens = null
                call.respond(provider.config)
            }
        }
    }
}
