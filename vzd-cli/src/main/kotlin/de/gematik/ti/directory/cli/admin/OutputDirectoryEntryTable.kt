package de.gematik.ti.directory.cli.admin

import de.gematik.ti.directory.admin.DirectoryEntry
import hu.vissy.texttable.dsl.tableFormatter

fun List<DirectoryEntry>.toTable(): String {
    val formatter = tableFormatter<DirectoryEntry> {
        labeled<String>("TelematikID", "Gesamt") {
            extractor { directoryEntry ->
                directoryEntry.directoryEntryBase.telematikID
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
                if (state.count > 99) {
                    "99+"
                } else {
                    state.count.toString()
                }
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
