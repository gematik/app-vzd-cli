/*
Copyright 2026, gematik GmbH

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

*******

For additional notes and disclaimer from gematik and in case of changes
by gematik, find details in the "Readme" file.
*/

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
