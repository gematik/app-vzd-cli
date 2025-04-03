package de.gematik.ti.directory.admin

import io.kotest.common.runBlocking
import io.kotest.core.spec.style.FeatureSpec
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

class TestQuickSearch :
    FeatureSpec({
        var client: Client? = null

        beforeSpec {
            client = createClient()
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
                    val result = client?.quickSearch("Siegfried Graf Otís Hamburg")?.directoryEntries ?: emptyList()
                    result.first().directoryEntryBase.telematikID shouldBe "1-SMC-B-Testkarte--883110000162002"
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

            scenario("Search by 'Berlin 5-'") {
                runBlocking {
                    val result = client?.quickSearch("Berlin 5-")?.directoryEntries ?: emptyList()
                    result.size shouldBeGreaterThan 0
                }
            }
            scenario("Search by '5- Berlin'") {
                runBlocking {
                    val result = client?.quickSearch("5- Berlin")?.directoryEntries ?: emptyList()
                    result.size shouldBeGreaterThan 0
                }
            }
        }
    })
