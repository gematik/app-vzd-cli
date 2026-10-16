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

package de.gematik.ti.directory.admin

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FeatureSpec
import io.kotest.matchers.ints.shouldBeLessThan
import kotlinx.datetime.Clock
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

class TestLog :
    FeatureSpec({
        val telematikID = "9-${javaClass.name}"
        feature("admin log") {
            scenario("Befehl 'admin log' ohne parameter liefert error") {
                shouldThrow<Exception> {
                    runCLI(listOf("admin", "tu", "log", "--json"))
                }
            }
            /*
            TODO: logs do not work in TU atm
            scenario("Aktualisierung eines Eintrages erzeugt einen neuen Log-Eintrag") {
                val logEntriesBefore =
                    Json.decodeFromString<List<LogEntry>>(
                        runCLI(listOf("admin", "tu", "log", "--json", "-t", telematikID)),
                    )
                try {
                    runCLI(listOf("admin", "tu", "add-base", "-s", "telematikID=$telematikID", "-s", "entryType=1", "-s", "postalCode=12345", "-s", "displayName=Test-vzd-cli", "-s", "specialization=urn:as:1.2.276.0.76.5.514:141002"))
                } catch (e: Exception) {
                    // OK to ignore this error
                }
                runCLI(listOf("admin", "tu", "modify-base-attr", "-t", telematikID, "-s", "meta=${Clock.System.now()}"))

                val logEntriesAfter =
                    Json.decodeFromString<List<LogEntry>>(
                        runCLI(listOf("admin", "tu", "log", "--json", "-t", telematikID)),
                    )
                logEntriesBefore.size shouldBeLessThan logEntriesAfter.size
            }
             */
        }
    })
