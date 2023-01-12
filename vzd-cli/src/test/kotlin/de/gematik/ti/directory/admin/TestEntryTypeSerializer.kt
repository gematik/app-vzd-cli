package de.gematik.ti.directory.admin

import io.kotest.core.spec.style.FeatureSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json


class TestEntryTypeSerializer : FeatureSpec({

    feature("Abwährtskompatibilität beim EntryType") {
        scenario("EntryType kann aus 'Int' ermittelt werden") {
            val json = """
                {
                    "telematikID": "9-test",
                    "entryType": 1
                }
            """.trimIndent()
            val baseEntry = Json.decodeFromString<BaseDirectoryEntry>(json)
            baseEntry.entryType shouldBe 1
        }
        scenario("EntryType kann aus 'List<Int>' ermittelt werden") {
            val json = """
                {
                    "telematikID": "9-test",
                    "entryType": [4, 5]
                }
            """.trimIndent()
            val baseEntry = Json.decodeFromString<BaseDirectoryEntry>(json)
            baseEntry.entryType shouldBe 4
        }
        scenario("EntryType darf null sein") {
            val json = """
                {
                    "telematikID": "9-test",
                    "entryType": null
                }
            """.trimIndent()
            val baseEntry = Json.decodeFromString<BaseDirectoryEntry>(json)
            baseEntry.entryType shouldBe null
        }
        scenario("EntryType mit leeren array führt zu null") {
            val json = """
                {
                    "telematikID": "9-test",
                    "entryType": []
                }
            """.trimIndent()
            val baseEntry = Json.decodeFromString<BaseDirectoryEntry>(json)
            baseEntry.entryType shouldBe null
        }
        scenario("EntryType serialize") {
            val baseEntry1 = BaseDirectoryEntry(telematikID = "9-test", entryType = 1)
            Json.encodeToString(baseEntry1) shouldBe """{"telematikID":"9-test","entryType":1}"""
        }

    }
})