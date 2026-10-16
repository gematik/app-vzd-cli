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
