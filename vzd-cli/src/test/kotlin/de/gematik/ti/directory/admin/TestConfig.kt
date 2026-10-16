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

import de.gematik.ti.directory.cli.admin.AdminConfigFileStore
import io.kotest.core.spec.style.FeatureSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import kotlin.io.path.Path
import kotlin.io.path.createTempDirectory
import kotlin.io.path.deleteIfExists

class TestConfig :
    FeatureSpec({
        val configDir = createTempDirectory()
        val configPath = Path(configDir.toString(), "directory-admin-test.yaml")

        beforeSpec {
            configPath.deleteIfExists()
        }

        feature("Neuen Config erzeugen") {
            scenario("Default config hat 3 Umgebungen") {
                val config = AdminConfigFileStore(configPath).value
                config.environments.size shouldBe 3
                config.environments.keys shouldContain "tu"
                configPath.deleteIfExists()
            }
            scenario("Beim resetten der Konfiguration wird die Konfig-Datei neu erzeugt") {
                configPath.toFile().exists() shouldBe false
                AdminConfigFileStore(configPath).reset()
                configPath.toFile().exists() shouldBe true
                configPath.deleteIfExists()
            }
        }

        feature("vzd-cli admin config") {
            scenario("vzd-cli admin config get") {
                cli("admin", "config", "get") {
                    it shouldContain "https://vzdpflege-ref.vzd.ti-dienste.de:9543"
                    it shouldContain "https://vzdpflege-test.vzd.ti-dienste.de:9543"
                }
            }
            scenario("vzd-cli admin config get environments.tu") {
                cli("admin", "config", "get", "environments.tu") {
                    it shouldNotContain "https://vzdpflege-ref.vzd.ti-dienste.de:9543"
                    it shouldContain "https://vzdpflege-test.vzd.ti-dienste.de:9543"
                }
            }
        }

        afterSpec {
        }
    })
