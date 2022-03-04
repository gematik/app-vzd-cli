package vzd.teststuite.admin

import io.kotest.core.spec.style.FeatureSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import vzd.admin.client.DirectoryEntry

class TestAdminList : FeatureSpec({
    feature("admin list") {
        scenario("Befehl 'admin list' ohne parameter liefert beliebige Liste von 101 Einträgen") {
            val output = runCLI(listOf("admin", "--short", "list"))
            output.split("\n").size shouldBe 101
        }
        scenario("Befehl 'admin list -p telematikID=5-SMC-B-Testkarte-883110000092568' liefert Bonifatiuskrankenhaus als JSON") {
            val output =
                runCLI(listOf("admin", "--json", "list", "-p", "telematikID=5-SMC-B-Testkarte-883110000092568"))
            val entry: List<DirectoryEntry>? = Json.decodeFromString(output)
            entry?.first()?.directoryEntryBase?.telematikID shouldBe "5-SMC-B-Testkarte-883110000092568"
        }
        scenario("Befehl 'admin list -p telematikID=5-SMC-B-Testkarte-883110000092568' liefert Bonifatiuskrankenhaus als YAML") {
            val output =
                runCLI(listOf("admin", "--yaml", "list", "-p", "telematikID=5-SMC-B-Testkarte-883110000092568"))
            output shouldContain "5-SMC-B-Testkarte-883110000092568"

            output shouldContain "5-SMC-B-Testkarte-883110000092568"

        }
    }
})
