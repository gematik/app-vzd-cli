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
