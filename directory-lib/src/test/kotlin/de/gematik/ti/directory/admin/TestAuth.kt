package de.gematik.ti.directory.admin

import de.gematik.ti.directory.DirectoryEnvironment
import io.kotest.core.spec.style.FeatureSpec
import io.kotest.matchers.comparables.shouldBeGreaterThan

class TestAuth :
    FeatureSpec({
        var firstRun = true
        feature("Custom KTOR Auth Plugin") {
            scenario("Static token") {
                val adminClient =
                    Client {
                        apiURL = DefaultConfig.environment(DirectoryEnvironment.tu).apiURL
                        auth {
                            accessToken {
                                System.getenv("TEST_ACCESS_TOKEN")
                            }
                        }
                    }
                val result = adminClient.quickSearch("2-")
                result.directoryEntries.size shouldBeGreaterThan 0
            }
            scenario("Renew token") {
                val adminClient =
                    Client {
                        apiURL = DefaultConfig.environment(DirectoryEnvironment.tu).apiURL
                        auth {
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
                val result = adminClient.readDirectoryEntry(mapOf("telematikID" to "2-*"))
                (result?.size ?: 0) shouldBeGreaterThan 0
            }
        }
    })
