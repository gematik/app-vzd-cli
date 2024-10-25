package de.gematik.ti.directory.cli.fhir

import de.gematik.ti.directory.fhir.toDirectoryEntries
import hu.vissy.texttable.dsl.tableFormatter
import org.hl7.fhir.r4.model.Bundle

fun Bundle.toTable(): String {
    val formatter =
        tableFormatter<de.gematik.ti.directory.fhir.FHIRDirectoryEntry> {
            labeled<String>("TelematikID", "Gesamt") {
                extractor { it.telematikID }
            }

            class State(
                var count: Int = 0
            )
            stateful<String, State>("Name") {
                initState { State() }
                extractor { directoryEntry, state ->
                    state.count += 1
                    directoryEntry.displayName ?: "N/A"
                }
                cellFormatter {
                    maxWidth = 24
                }
                aggregator { _, state ->
                    state.count.toString()
                }
            }

            stateless("Address") {
                extractor { directoryEntry ->
                    buildString {
                        directoryEntry.location?.firstOrNull()?.let {
                            append(it.address?.line?.joinToString())
                            append(", ")
                            append(it.address?.postalCode)
                            append(" ")
                            append(it.address?.city)
                        }
                    }
                }
                cellFormatter {
                    maxWidth = 40
                }
            }

            showAggregation = true
        }

    return formatter.apply(this.toDirectoryEntries())
}
