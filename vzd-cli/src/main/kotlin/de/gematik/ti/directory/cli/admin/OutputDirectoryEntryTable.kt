/*
Copyright 2026, gematik GmbH

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

*******

For additional notes and disclaimer from gematik and in case of changes
by gematik, find details in the "Readme" file.
*/

package de.gematik.ti.directory.cli.admin

import de.gematik.ti.directory.admin.DirectoryEntry
import de.gematik.ti.directory.util.escape
import hu.vissy.texttable.dsl.tableFormatter

fun List<DirectoryEntry>.toTable(): String {
    val formatter =
        tableFormatter<DirectoryEntry> {
            labeled<String>("TelematikID", "Gesamt") {
                extractor { directoryEntry ->
                    directoryEntry.directoryEntryBase.telematikID.escape()
                }
            }

            class State(
                var count: Int = 0
            )
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
