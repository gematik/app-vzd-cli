package de.gematik.ti.directory.bff.dev

import de.gematik.ti.directory.admin.AdminEnvironment
import de.gematik.ti.directory.admin.Client
import de.gematik.ti.directory.admin.DefaultConfig
import de.gematik.ti.directory.bff.SearchResultsRepresentation
import de.gematik.ti.directory.bff.directoryApplicationModule
import io.kotest.core.spec.style.FeatureSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.testing.*

class TestSearch : FeatureSpec({
    val adminClient = Client {
        apiURL = DefaultConfig.environment(AdminEnvironment.tu).apiURL
        auth {
            accessToken { System.getenv()["TEST_ACCESS_TOKEN"] }
        }
    }
    feature("Search") {
        scenario("Search entries") {
            testApplication {
                application {
                    directoryApplicationModule(adminClient)
                }
                val client = createClient {
                    install(ContentNegotiation) {
                        json()
                    }
                }
                apply {
                    val response = client.get("/api/search") {
                        parameter("q", "noone_lives_here_anymore")
                    }
                    response.status shouldBe HttpStatusCode.OK
                    val searchResults = response.body<SearchResultsRepresentation>()
                    searchResults.searchQuery shouldBe "noone_lives_here_anymore"
                    searchResults.directoryEntries.size shouldBe 0
                }
                apply {
                    val response = client.get("/api/search") {
                        parameter("q", "1-")
                    }
                    response.status shouldBe HttpStatusCode.OK
                    val searchResults = response.body<SearchResultsRepresentation>()
                    searchResults.searchQuery shouldBe "1-"
                    searchResults.directoryEntries.size shouldBe 100
                }
            }
        }
    }
})
