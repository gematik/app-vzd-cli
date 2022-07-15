package vzd.teststuite.admin

import io.kotest.core.spec.style.FeatureSpec
import vzd.admin.client.Client

class TestPaging : FeatureSpec({
    var client: Client? = null

    beforeSpec {
        client = createClient()
    }

    feature("Suche nach Einträgen mit Paging") {
        scenario("Suche und finde mehr als 25 Eintröge in 5er Blocks") {
            client?.readDirectoryEntryForSyncPaging(mapOf("telematikID" to "9-*"))
        }
    }

    afterSpec {
    }
})
