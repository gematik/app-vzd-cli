/*
 * Copyright (Change Date see Readme), gematik GmbH
 *
 * Licensed under the EUPL, Version 1.2 or - as soon they will be approved by the
 * European Commission – subsequent versions of the EUPL (the "Licence").
 * You may not use this work except in compliance with the Licence.
 *
 * You find a copy of the Licence in the "Licence" file or at
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either expressed or implied.
 * In case of changes by gematik find details in the "Readme" file.
 *
 * See the Licence for the specific language governing permissions and limitations under the Licence.
 *
 * *******
 *
 * For additional notes and disclaimer from gematik and in case of changes by gematik,
 * find details in the "Readme" file.
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
