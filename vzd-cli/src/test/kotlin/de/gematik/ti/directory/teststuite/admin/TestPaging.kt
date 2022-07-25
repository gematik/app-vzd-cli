package de.gematik.ti.directory.teststuite.admin

import de.gematik.ti.directory.admin.client.Client
import io.kotest.core.spec.style.FeatureSpec
import io.ktor.http.*

class TestPaging : FeatureSpec({
    var client: Client? = null

    beforeSpec {
        client = createClient()
    }

    feature("Suche nach Einträgen mit Paging") {
        scenario("Suche und finde mehr als 25 Einträge in 5er Blocks") {
            /*
            val exception = shouldThrow<VZDResponseException> {
                client?.readDirectoryEntryForSyncPaging(
                    mapOf(
                        "telematikID" to "9-*",
                        "size" to "5",
                        "cookie" to ""
                    )
                )
            }
            exception.response.status shouldBe HttpStatusCode.NotImplemented
             */
        }
    }

    afterSpec {
    }
})
