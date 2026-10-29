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
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

class TestServerErrorHints :
    FeatureSpec({
        feature("Explain known server errors") {
            scenario("entryType mismatch with OID") {
                explainServerError("entryType mismatch", professionOIDs = listOf("1.2.276.0.76.4.54"))!! shouldContain "entryType 3"
            }
            scenario("entryType mismatch without context lists the table") {
                explainServerError("entryType mismatch")!! shouldContain "3 Leistungserbringerinstitution"
            }
            scenario("HealthcareSpeciality for pharmacy") {
                val hint = explainServerError("HealthcareSpeciality mismatch", telematikID = "3-01.2.2023000")!!
                hint shouldContain "10 Offizin-Apotheke"
                hint shouldContain "offizin-apotheke"
            }
            scenario("HealthcareSpeciality without sector lists all formats") {
                val hint = explainServerError("HealthcareSpeciality mismatch")!!
                hint shouldContain "urn:psc:"
                hint shouldContain "urn:as:"
            }
            scenario("KOMLE") { explainServerError("Error when KOMLECertPolicies-check!")!! shouldContain "C.HCI.ENC" }
            scenario("stateOrProvinceName") { explainServerError("stateOrProvinceName invalid")!! shouldContain "Schleswig-Holstein" }
            scenario("Unknown error") { explainServerError("Bad response") shouldBe null }
        }
    })
