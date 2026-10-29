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

package de.gematik.ti.directory.fhir

import de.gematik.ti.directory.admin.BaseDirectoryEntry
import de.gematik.ti.directory.admin.DirectoryEntry
import de.gematik.ti.directory.elaborate.elaborate
import io.kotest.core.spec.style.FeatureSpec
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class TestSimpleFHIR :
    FeatureSpec({
        feature("Very basic FHIR parser") {
            scenario("Parse CodeSystem") {
                OrganizationProfessionOID.name shouldBe "OrganizationProfessionOID"
                OrganizationProfessionOID.concept.size shouldBeGreaterThan 5
                OrganizationProfessionOID.resolveCode("1.2.276.0.76.4.55")?.display shouldBe "Krankenhausapotheke"
            }

            scenario("Parse and use ValueSet") {
                val specialization = "urn:psc:1.3.6.1.4.1.19376.3.276.1.5.4:ALLG"
                val regex = Regex("^urn:psc:([0-9\\.]+):(.*)$")
                regex.matchEntire(specialization)?.let {
                    HealthcareServiceTypeVS
                        .resolveCode(
                            "urn:oid:" + it.groupValues[1],
                            it.groupValues[2],
                        )?.display shouldBe "Allgemeinmedizin"
                }
                PractitionerQualificationVS.resolveCode("urn:oid:1.2.276.0.76.5.114", "010")?.display shouldBe "FA Allgemeinmedizin"
            }

            scenario("PharmacyTypeLDAPCS codes") {
                PharmacyTypeLDAPCS.concept.map { it.code } shouldBe listOf("10", "20", "30", "40", "50")
                PharmacyTypeLDAPCS.resolveCode("10")?.display shouldBe "Offizin-Apotheke"
            }

            scenario("Region has 18 values") {
                Region.concept.size shouldBe 18
                Region.resolveCode("Schleswig-Holstein") shouldNotBe null
            }

            scenario("Pharmacy specialization resolves to LDAP code system") {
                val entry = DirectoryEntry(BaseDirectoryEntry(telematikID = "3-01.2.2023000", specialization = listOf("10")))
                entry.elaborate().base.specialization?.first()?.display shouldBe "Offizin-Apotheke"
            }
        }
    })
