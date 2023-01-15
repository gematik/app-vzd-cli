package de.gematik.ti.directory.admin

import io.kotest.core.spec.style.FeatureSpec
import io.kotest.matchers.comparables.shouldBeGreaterThan
import io.kotest.matchers.shouldBe

class TestAuth : FeatureSpec({
    var firstRun = true
    feature("Custom KTOR Auth Plugin") {
        scenario("Static token") {
            val adminClient = Client {
                apiURL = DefaultConfig.environment(AdminEnvironment.tu).apiURL
                auth {
                    val authURL = DefaultConfig.environment(AdminEnvironment.tu).authURL
                    accessToken {
                        System.getenv("TEST_ACCESS_TOKEN")
                    }
                }
            }
            val result = adminClient.quickSearch("2-")
            result.directoryEntries.size shouldBeGreaterThan 0
        }
        scenario("Renew token") {
            val adminClient = Client {
                apiURL = DefaultConfig.environment(AdminEnvironment.tu).apiURL
                auth {
                    val authURL = DefaultConfig.environment(AdminEnvironment.tu).authURL
                    accessToken {
                        if (firstRun) {
                            firstRun = false
                            null
                        } else {
                            System.getenv("TEST_ACCESS_TOKEN")
                        }
                    }
                }
            }
            val result = adminClient.readDirectoryEntry(mapOf("telematikID" to "2-SMC-B-Testkarte-883110000103275"))
            result?.size shouldBe 1
        }
    }
})
