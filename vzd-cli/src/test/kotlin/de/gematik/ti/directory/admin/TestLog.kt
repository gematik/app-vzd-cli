package de.gematik.ti.directory.admin

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FeatureSpec
import io.kotest.matchers.ints.shouldBeLessThan
import kotlinx.datetime.Clock
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

class TestLog : FeatureSpec({
    val telematikID = "9-${javaClass.name}"
    feature("admin log") {
        scenario("Befehl 'admin log' ohne parameter liefert error") {
            shouldThrow<Exception> {
                runCLI(listOf("admin", "tu", "log", "--json"))
            }
        }
        scenario("Aktualisierung eines Eintrages erzeugt einen neuen Log-Eintrag") {
            val logEntriesBefore = Json.decodeFromString<List<LogEntry>>(runCLI(listOf("admin", "tu", "log", "--json", "-t", telematikID)))
            try {
                runCLI(listOf("admin", "tu", "add-base", "-s", "telematikID=$telematikID", "-s", "entryType=1"))
            } catch (e: Exception) {
                // OK to ignore this error
            }
            runCLI(listOf("admin", "tu", "modify-base-attr", "-t", telematikID, "-s", "meta=${Clock.System.now()}"))

            val logEntriesAfter = Json.decodeFromString<List<LogEntry>>(runCLI(listOf("admin", "tu", "log", "--json", "-t", telematikID)))
            logEntriesBefore.size shouldBeLessThan logEntriesAfter.size
        }
    }
})
