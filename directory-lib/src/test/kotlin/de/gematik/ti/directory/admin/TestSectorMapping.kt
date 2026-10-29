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

import io.kotest.core.spec.style.FeatureSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe

class TestSectorMapping :
    FeatureSpec({
        feature("Sector by TelematikID") {
            scenario("Pharmacy") { SectorMapping.sectorOf("3-01.2.2023000")?.table shouldBe 4 }
            scenario("EU mail-order pharmacy") { SectorMapping.sectorOf("9-2.54.123")?.table shouldBe 4 }
            scenario("KV practice wins over BÄK") { SectorMapping.sectorOf("1-2012345678")?.table shouldBe 2 }
            scenario("Test card falls into BÄK by ILF regex") {
                SectorMapping.sectorOf("1-SMC-B-Testkarte-883110000117035")?.table shouldBe 8
            }
            scenario("Issue TelematikID has no sector") { SectorMapping.sectorOf("3-2-Test01") shouldBe null }
            scenario("Hospital") { SectorMapping.sectorOf("5-2-KHAUS-123")?.specializationFormat shouldBe SpecializationFormat.PSC }
            scenario("eGBR institution requires state") {
                SectorMapping.sectorOf("10-67.245.1")!!.mandatory shouldContain "stateOrProvinceName"
            }
            scenario("All 19 ILF tables are loaded in order") {
                SectorMapping.sectors.map { it.table } shouldBe (2..20).toList()
            }
        }
    })
