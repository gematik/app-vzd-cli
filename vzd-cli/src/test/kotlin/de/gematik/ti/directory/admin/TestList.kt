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

import io.kotest.core.spec.style.FeatureSpec
import io.kotest.matchers.shouldBe

class TestList :
    FeatureSpec({
        feature("admin list") {
            scenario("Befehl 'admin list' ohne parameter liefert beliebige Liste von 100 Einträgen") {
                val output = runCLI(listOf("admin", "tu", "list", "--table"))
                output.trim().split("\n").size shouldBe 106
            }
        /*
        scenario("Befehl 'admin list -p telematikID=5-SMC-B-Testkarte-883110000092568' liefert Bonifatiuskrankenhaus als JSON") {
            val output =
                runCLI(listOf("admin", "tu", "list", "--json", "-p", "telematikID=5-SMC-B-Testkarte-883110000092568"))
            val entry: List<DirectoryEntry>? = Json.decodeFromString(output)
            entry?.first()?.directoryEntryBase?.telematikID shouldBe "5-SMC-B-Testkarte-883110000092568"
        }
        scenario("Befehl 'admin list -p telematikID=5-SMC-B-Testkarte-883110000092568' liefert Bonifatiuskrankenhaus als YAML") {
            val output =
                runCLI(listOf("admin", "tu", "list", "--yaml", "-p", "telematikID=5-SMC-B-Testkarte-883110000092568"))
            output shouldContain "5-SMC-B-Testkarte-883110000092568"

            output shouldContain "5-SMC-B-Testkarte-883110000092568"
        }
         */
        }
    })
