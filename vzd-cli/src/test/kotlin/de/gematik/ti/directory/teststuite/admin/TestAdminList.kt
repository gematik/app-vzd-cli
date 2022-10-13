package de.gematik.ti.directory.teststuite.admin

import de.gematik.ti.directory.admin.DirectoryEntry
import io.kotest.core.spec.style.FeatureSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

class TestAdminList : FeatureSpec({
    feature("admin list") {
        scenario("Befehl 'admin list' ohne parameter liefert beliebige Liste von 100 Einträgen") {
            val output = runCLI(listOf("admin", "--env=tu", "list", "--table"))
            output.trim().split("\n").size shouldBe 104
        }
        scenario("Befehl 'admin list -p telematikID=5-SMC-B-Testkarte-883110000092568' liefert Bonifatiuskrankenhaus als JSON") {
            val output =
                runCLI(listOf("admin", "--env=tu", "--json", "list", "-p", "telematikID=5-SMC-B-Testkarte-883110000092568"))
            val entry: List<DirectoryEntry>? = Json.decodeFromString(output)
            entry?.first()?.directoryEntryBase?.telematikID shouldBe "5-SMC-B-Testkarte-883110000092568"
        }
        scenario("Befehl 'admin list -p telematikID=5-SMC-B-Testkarte-883110000092568' liefert Bonifatiuskrankenhaus als YAML") {
            val output =
                runCLI(listOf("admin", "--env=tu", "--yaml", "list", "-p", "telematikID=5-SMC-B-Testkarte-883110000092568"))
            output shouldContain "5-SMC-B-Testkarte-883110000092568"

            output shouldContain "5-SMC-B-Testkarte-883110000092568"
        }
    }
})
