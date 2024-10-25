package de.gematik.ti.directory.admin

import io.kotest.common.runBlocking
import io.kotest.core.spec.style.FeatureSpec
import io.kotest.matchers.shouldBe
import io.ktor.http.*

class TestPaging :
    FeatureSpec({
        var client: Client? = null

        beforeSpec {
            client = createClient()
        }

        feature("Suche nach Einträgen mit Paging") {
            scenario("Suche und finde mehr als 3 Einträge in 3er Blocks") {
                runBlocking {
                    @Suppress("DEPRECATION")
                    val withOutPaging =
                        client
                            ?.readDirectoryEntryForSync(
                                mapOf(
                                    "telematikID" to "9-2*",
                                ),
                            )?.size
                    var withPaging = 0
                    client?.streamDirectoryEntriesPaging(
                        mapOf(
                            "telematikID" to "9-2*",
                        ),
                        100,
                    ) {
                        withPaging++
                    }
                    withPaging shouldBe withOutPaging
                }
            }
        }

        afterSpec {
        }
    })
