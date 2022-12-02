package de.gematik.ti.directory.admin

import io.kotest.common.runBlocking
import io.kotest.core.spec.style.FeatureSpec
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

class TestQuickSearch : FeatureSpec({
    var client: Client? = null

    beforeSpec {
        //client = createClient()
    }

    feature("POSTokenizer") {
        scenario("Support for 3 words localities") {
            val result = POSTokenizer.tokenize("Frankfurt am Main")
            result.tokens.size shouldBe 3
            result.positions.size shouldBe 1
            result.positions[0].type shouldBe TokenType.LocalityName
        }

        scenario("Support for 2 words localities") {
            val result = POSTokenizer.tokenize("Bad Belzig")
            result.tokens.size shouldBe 2
            result.positions.size shouldBe 1
            result.positions.first().type shouldBe TokenType.LocalityName
        }

        scenario("Tokenize one word locality") {
            val result = POSTokenizer.tokenize("Berlin")
            result.tokens.size shouldBe 1
            result.positions.size shouldBe 1
            result.positions.first().type shouldBe TokenType.LocalityName
        }

        scenario("Tokenize mixed localities and other tokens #1") {
            val result = POSTokenizer.tokenize("Frankfurt am Main Rumpelstiltskin")
            result.tokens.size shouldBe 4
            result.positions.size shouldBe 2
            result.positions.count { it.type == TokenType.Plain} shouldBe 1
            result.positions.count { it.type == TokenType.LocalityName} shouldBe 1
        }

        scenario("Tokenize mixed localities and other tokens #2") {
            val result = POSTokenizer.tokenize("Rumpelstiltskin Frankfurt am Main")
            result.tokens.size shouldBe 4
            result.positions.size shouldBe 2
            result.positions.count { it.type == TokenType.Plain} shouldBe 1
            result.positions.count { it.type == TokenType.LocalityName} shouldBe 1
        }

        scenario("Tokenize mixed localities and other tokens #3") {
            val result = POSTokenizer.tokenize("Dr. Rumpelstiltskin Grimm Frankfurt am Main")
            result.tokens.size shouldBe 6
            result.positions.size shouldBe 4
            result.positions.count { it.type == TokenType.Plain} shouldBe 3
            result.positions.count { it.type == TokenType.LocalityName} shouldBe 1
        }

        scenario("Tokenize mixed localities and other tokens #4") {
            val result = POSTokenizer.tokenize("Dr. Rumpelstiltskin Grimm Frankfurt am Main")
            result.tokens.size shouldBe 6
            result.positions.size shouldBe 4
            result.positions.count { it.type == TokenType.Plain} shouldBe 3
            result.positions.count { it.type == TokenType.LocalityName} shouldBe 1
        }

        scenario("Tokenize mixed localities and other tokens #5") {
            val result = POSTokenizer.tokenize("Sieglinde Berlin")
            result.tokens.size shouldBe 2
            result.positions.size shouldBe 2
            result.positions.count { it.type == TokenType.LocalityName} shouldBe 1
            result.positions.count { it.type == TokenType.Plain} shouldBe 1
        }

        scenario("Tokenize mixed localities and other tokens #6") {
            val result = POSTokenizer.tokenize("Dr. Rumpelstiltskin Grimm Frankfurt am Main 60306")
            result.tokens.size shouldBe 7
            result.positions.size shouldBe 5
            result.positions.count { it.type == TokenType.Plain} shouldBe 3
            result.positions.count { it.type == TokenType.LocalityName} shouldBe 1
            result.positions.count { it.type == TokenType.PostalCode} shouldBe 1
        }

        scenario("Tokenize mixed localities and other tokens #7") {
            val result = POSTokenizer.tokenize("Berlin Christian Köln")
            println(result)
            result.tokens.size shouldBe 3
            result.positions.size shouldBe 3
            result.positions.count { it.type == TokenType.Plain} shouldBe 1
            result.positions.count { it.type == TokenType.LocalityName} shouldBe 2
        }

        scenario("Tokenize mixed localities and other tokens #8") {
            val result = POSTokenizer.tokenize("Berlin Christian Köln 13555")
            result.tokens.size shouldBe 4
            result.positions.size shouldBe 4
            result.positions.count { it.type == TokenType.Plain} shouldBe 1
            result.positions.count { it.type == TokenType.LocalityName} shouldBe 2
            result.positions.count { it.type == TokenType.PostalCode} shouldBe 1

            result.join(result.positions.first { it.type == TokenType.LocalityName}) shouldBe "Berlin"
        }

        scenario("Tokenize one word string, not locality") {
            val result = POSTokenizer.tokenize("Rumpelstiltskin")
            result.tokens.size shouldBe 1
            result.positions.size shouldBe 1
            result.positions.first().type shouldBe TokenType.Plain
        }

        scenario("Tokenize by locality, which is present als plain name and name with additions") {
            val result1 = POSTokenizer.tokenize("Herzberg am Harz")
            println(result1.positions)
            result1.tokens.size shouldBe 3
            result1.positions.size shouldBe 1
            result1.positions.first().type shouldBe TokenType.LocalityName
            val result2 = POSTokenizer.tokenize("Herzberg")
            result2.tokens.size shouldBe 1
            result2.positions.size shouldBe 1
            result2.positions.first().type shouldBe TokenType.LocalityName
        }
    }

    feature("Quick Search") {
        scenario("Search by localityName") {
            runBlocking {
                val result = client?.quickSearch("Berlin")?.directoryEntries ?: emptyList()
                result.size shouldBeGreaterThan 10
            }
        }
        scenario("Search by displayName and locationName") {
            runBlocking {
                val result = client?.quickSearch("Siegfried Klön Berlin")?.directoryEntries ?: emptyList()
                result.first().directoryEntryBase.telematikID shouldBe "1-SMC-B-Testkarte-883110000100535"
            }
        }
        scenario("Search by postalCode") {
            runBlocking {
                val result = client?.quickSearch("12526")?.directoryEntries ?: emptyList()
                result.size shouldBeGreaterThan 1
                result.first().directoryEntryBase.postalCode shouldBe "12526"
            }
        }

        scenario("Search by localityName Bad Tölz") {
            runBlocking {
                val result = client?.quickSearch("Bad Tölz")?.directoryEntries ?: emptyList()
                result.size shouldBeGreaterThan 1
                result.first().directoryEntryBase.localityName shouldBe "Bad Tölz"
            }
        }

        scenario("Search 'Bad Tölz St. Vincenz' ") {
            runBlocking {
                val result = client?.quickSearch("Bad Tölz St. Vincenz")?.directoryEntries ?: emptyList()
                result.size shouldBeGreaterThan 0
                result.first().directoryEntryBase.localityName shouldBe "Bad Tölz"
                result.first().directoryEntryBase.displayName shouldContain "St. Vincenz"
            }
        }
    }
})
