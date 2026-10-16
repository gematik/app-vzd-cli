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
