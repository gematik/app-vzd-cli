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
