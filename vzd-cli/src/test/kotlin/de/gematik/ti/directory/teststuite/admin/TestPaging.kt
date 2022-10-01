package de.gematik.ti.directory.teststuite.admin

import de.gematik.ti.directory.admin.Client
import io.kotest.common.runBlocking
import io.kotest.core.spec.style.FeatureSpec
import io.kotest.matchers.shouldBe
import io.ktor.http.*

class TestPaging : FeatureSpec({
    var client: Client? = null

    beforeSpec {
        client = createClient()
    }

    feature("Suche nach Einträgen mit Paging") {
        scenario("Suche und finde mehr als 3 Einträge in 3er Blocks") {
            runBlocking {
                val withOutPaging = client?.readDirectoryEntryForSync(
                    mapOf(
                        "telematikID" to "9-*"
                    )
                )?.size
                var withPaging = 0
                client?.streamDirectoryEntriesPaging(
                    mapOf(
                        "telematikID" to "9-*"
                    ),
                    3
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
