package de.gematik.ti.directory.bff.dev

import de.gematik.ti.directory.admin.Client
import de.gematik.ti.directory.bff.BuildConfig
import de.gematik.ti.directory.bff.InfoResource
import de.gematik.ti.directory.bff.directoryApplicationModule
import io.kotest.core.spec.style.FeatureSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.testing.*

class TestInfo : FeatureSpec({
    val adminClient = Client {
        auth {
            accessToken { System.getenv()["TEST_ACCESS_TOKEN"] }
        }
    }
    feature("Information about the API") {
        scenario("Version and capabilities") {
            testApplication {
                application {
                    directoryApplicationModule(adminClient)
                }
                val client = createClient {
                    install(ContentNegotiation) {
                        json()
                    }
                }
                val response = client.get("/api/info")
                response.status shouldBe HttpStatusCode.OK
                val info = response.body<InfoResource>()
                info.version shouldBe BuildConfig.APP_VERSION
                info.capabilities shouldContain "core"
            }
        }
    }
})
