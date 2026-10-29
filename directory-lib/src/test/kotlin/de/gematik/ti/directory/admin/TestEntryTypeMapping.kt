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

import de.gematik.ti.directory.pki.TestCertificates
import io.kotest.core.spec.style.FeatureSpec
import io.kotest.matchers.shouldBe

class TestEntryTypeMapping :
    FeatureSpec({
        feature("professionOID to entryType") {
            scenario("Öffentliche Apotheke is 3") { EntryTypeMapping.entryTypeOf("1.2.276.0.76.4.54") shouldBe 3 }
            scenario("Arzt is 1") { EntryTypeMapping.entryTypeOf("1.2.276.0.76.4.30") shouldBe 1 }
            scenario("Unknown OID") { EntryTypeMapping.entryTypeOf("1.2.276.0.76.4.9999") shouldBe null }
            scenario("Two HBA professions agree") {
                EntryTypeMapping.entryTypeOf(listOf("1.2.276.0.76.4.45", "1.2.276.0.76.4.46")) shouldBe 1
            }
            scenario("Disagreeing OIDs give null") {
                EntryTypeMapping.entryTypeOf(listOf("1.2.276.0.76.4.30", "1.2.276.0.76.4.54")) shouldBe null
            }
            scenario("describe uses CodeSystem display") {
                EntryTypeMapping.describe("1.2.276.0.76.4.54") shouldBe "1.2.276.0.76.4.54 (Öffentliche Apotheke)"
            }
            scenario("describe falls back to the OID") {
                EntryTypeMapping.describe("1.2.276.0.76.4.9999") shouldBe "1.2.276.0.76.4.9999"
            }
            scenario("nameOf") { EntryTypeMapping.nameOf(3) shouldBe "Leistungserbringerinstitution" }
        }
        feature("CertificateInfo") {
            scenario("SMC-B ENC exposes type and entryType") {
                val info = TestCertificates.load(TestCertificates.smcbRsaEnc).certificateInfo
                info.certificateType shouldBe "C.HCI.ENC"
                info.entryType shouldBe "3"
            }
            scenario("HBA ENC with two professions") {
                val info = TestCertificates.load(TestCertificates.hbaEnc).certificateInfo
                info.certificateType shouldBe "C.HP.ENC"
                info.entryType shouldBe "1"
            }
        }
    })
