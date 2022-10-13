package de.gematik.ti.directory.cli.gui

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.output.TermUi
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.int
import de.gematik.ti.directory.admin.Client
import de.gematik.ti.directory.admin.FileConfigProvider
import de.gematik.ti.directory.admin.quickSearch
import de.gematik.ti.directory.util.TokenStore
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
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.URL

class GuiCommand : CliktCommand(name = "gui", help = """Starts HTTP Server with GUI""".trimMargin()) {
    val port by option("-p", "--port").int().default(57036)

    override fun run() {
        embeddedServer(Netty, port = port, host = "127.0.0.1") {
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
                    call.respondText(text = "401: Unauthorized (token expired)", status = HttpStatusCode.Unauthorized)
                }
            }
            environment.monitor.subscribe(ApplicationStarted) { application ->
                TermUi.echo("Starting server at: http://127.0.0.1:$port")
                val url = "http://127.0.0.1:$port/api/admin/config"
                val os = System.getProperty("os.name").lowercase()
                launch {
                    repeat(10) {
                        Thread.sleep(250)
                        try {
                            URL(url).openConnection()
                            if (os.contains("win")) {
                                Runtime.getRuntime().exec("start $url")
                            } else if (os.contains("mac")) {
                                Runtime.getRuntime().exec("open $url")
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

@Resource("/{env}/search")
@Serializable
data class SearchResource(val env: String, val q: String)

@Resource("/{env}/DirectoryEntry/{telematikID}")
@Serializable
data class DirectoryEntryResource(val env: String, val telematikID: String)

fun downstreamClient(env: String): Client {
    val config = FileConfigProvider().config
    val tokenStore = TokenStore()
    val envConfig = config.environment(env) ?: throw Exception("Unknown environment: $env")
    val client = Client() {
        apiURL = envConfig.apiURL
        accessToken = tokenStore.accessTokenFor(envConfig.apiURL) ?: throw Exception("Token : $env")
        if (config.httpProxy.enabled) {
            httpProxyURL = config.httpProxy.proxyURL
        }
    }
    return client
}

fun Application.configureSearchRouting() {
    routing { }() {
        route("api") {
            route("admin") {
                get("config") {
                    val provider = FileConfigProvider()
                    call.respond(provider.config)
                }

                get<SearchResource> { search ->
                    val client = downstreamClient(search.env)
                    call.respond(client.quickSearch(search.q))
                }
                get<DirectoryEntryResource> { entry ->
                    val client = downstreamClient(call.parameters.get("env")!!)
                    val entries = client.readDirectoryEntry(mapOf("telematikID" to entry.telematikID))

                    entries?.first()?.also {
                        call.respond(it)
                    }
                }
            }
        }
    }
}