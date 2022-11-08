package de.gematik.ti.directory.cli.apo

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.requireObject
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.multiple
import de.gematik.ti.directory.cli.catching
import hu.vissy.texttable.dsl.tableFormatter
import io.ktor.client.*
import io.ktor.client.engine.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import org.hl7.fhir.r4.model.Location

class SearchCommand : CliktCommand(name = "search", help = "Search for pharmacies in ApoVZD") {
    private val context by requireObject<ApoInstanceCliContext>()
    private val arguments by argument().multiple()

    fun run2() = catching {
        val http = HttpClient(CIO) {
            engine {
                // proxy = ProxyBuilder.http("http://192.168.110.10:3128/")
            }
        }

        runBlocking {
            val response = http.get("https://apovzd.app.ti-dienste.de/api/") {
                url {
                    appendPathSegments("Location")
                }
            }
            echo(response)
        }
    }
    override fun run() = catching {
        val queryString = arguments.joinToString(" ")
        val bundle = runBlocking {
            context.client.search(queryString)?.second
        }

        val formatter = tableFormatter<Location> {
            labeled("TelematikID", "Gesamt") {
                extractor { location ->
                    location.identifier.firstOrNull {
                        it.system == "https://gematik.de/fhir/NamingSystem/TelematikID"
                    }?.value
                }
            }

            class State(var count: Int = 0)
            stateful<String, State>("Name") {
                initState { State() }
                extractor { location, state ->
                    state.count += 1
                    location.name
                }
                cellFormatter {
                    maxWidth = 24
                }
                aggregator { _, state ->
                    if (state.count > 79) {
                        "79+"
                    } else {
                        state.count.toString()
                    }
                }
            }

            stateless("Address") {
                extractor { location ->
                    buildString {
                        append(location.address?.line?.firstOrNull() ?: "n/a")
                        append(" ")
                        append(location.address.postalCode ?: "n/a")
                        append(" ")
                        append(location.address.city ?: "n/a")
                    }
                }
                cellFormatter {
                    maxWidth = 40
                }
            }

            showAggregation = true
        }
        println(formatter.apply(bundle?.entry?.mapNotNull { it.resource }?.filterIsInstance<Location>() ?: emptyList()))
    }
}
