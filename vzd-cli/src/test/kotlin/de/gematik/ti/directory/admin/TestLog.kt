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
