package de.gematik.ti.directory.admin

import io.kotest.common.runBlocking
import io.kotest.core.spec.style.FeatureSpec
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

class TestQuickSearch : FeatureSpec({
    var client: Client? = null

    beforeSpec {
        client = createClient()
    }

    feature("Tokenizer is able to detect multi-word locality") {
        scenario("Support for 3 words localities") {
            val tokens = Tokenizer.tokenize("Frankfurt am Main")
            tokens.size shouldBe 1
            tokens[0].type shouldBe TokenType.LocalityName
        }
        scenario("Tokenize by locality, which is present als plain name and name with additions") {
            val tokens = Tokenizer.tokenize("Herzberg am Hartz")
            println(tokens.map { "${it.type}=${it.value}" })
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
