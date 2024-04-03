package de.gematik.ti.directory.cli.fhir

import de.gematik.ti.directory.admin.DirectoryEntry
import de.gematik.ti.directory.util.escape
import hu.vissy.texttable.dsl.tableFormatter
import org.hl7.fhir.r4.model.Bundle

fun Bundle.toTable(): String {
    val formatter =
        tableFormatter<Bundle> {
            labeled<String>("TelematikID", "Gesamt") {
                extractor { directoryEntry ->
                    "directoryEntry.directoryEntryBase.telematikID.escape()"
                }
            }

            class State(var count: Int = 0)
            stateful<String, State>("Name") {
                initState { State() }
                extractor { directoryEntry, state ->
                    state.count += 1
                    directoryEntry.directoryEntryBase.displayName
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
                        append(directoryEntry.directoryEntryBase.streetAddress ?: "n/a")
                        append(" ")
                        append(directoryEntry.directoryEntryBase.postalCode ?: "n/a")
                        append(" ")
                        append(directoryEntry.directoryEntryBase.localityName ?: "n/a")
                    }
                }
                cellFormatter {
                    maxWidth = 40
                }
            }

            showAggregation = true
        }

    return formatter.apply(this)
}
