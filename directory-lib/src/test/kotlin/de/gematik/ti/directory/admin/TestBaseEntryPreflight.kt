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

import de.gematik.ti.directory.validation.FindingSeverity
import io.kotest.core.spec.style.FeatureSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain as shouldContainText

private fun errors(f: List<PreflightFinding>) = f.filter { it.severity == FindingSeverity.ERROR }.map { it.attribute }

private fun warnings(f: List<PreflightFinding>) = f.filter { it.severity == FindingSeverity.WARNING }.map { it.attribute }

class TestBaseEntryPreflight :
    FeatureSpec({
        val pharmacy =
            BaseDirectoryEntry(
                "3-01.2.2023000",
                displayName = "Linden Apotheke",
                streetAddress = "Hauptstr. 1",
                postalCode = "10117",
                localityName = "Berlin",
                countryCode = "DE",
                specialization = listOf("10"),
            )

        feature("Generic rules") {
            scenario("Complete pharmacy has no errors") { errors(preflight(pharmacy)) shouldBe emptyList() }
            scenario("Address without countryCode") {
                val f = preflight(pharmacy.copy(countryCode = null))
                errors(f) shouldBe listOf("countryCode")
                f.single { it.attribute == "countryCode" }.suggestion shouldBe "DE"
            }
            scenario("Four digit PLZ") { errors(preflight(pharmacy.copy(postalCode = "1011"))) shouldBe listOf("postalCode") }
            scenario("State not in Region") {
                val f = preflight(pharmacy.copy(stateOrProvinceName = "Schleswig Holstein")).single { it.attribute == "stateOrProvinceName" }
                f.severity shouldBe FindingSeverity.ERROR
                f.allowedValues!! shouldContain "Schleswig-Holstein"
            }
            scenario("Valid state passes") { errors(preflight(pharmacy.copy(stateOrProvinceName = "Berlin"))) shouldBe emptyList() }
            scenario("Short displayName") { errors(preflight(pharmacy.copy(displayName = "-"))) shouldBe listOf("displayName") }
            scenario("Unknown sector only warns") {
                val f = preflight(BaseDirectoryEntry("3-2-Test01", displayName = "Test"))
                errors(f) shouldBe emptyList()
                warnings(f) shouldBe listOf("telematikID")
            }
        }

        feature("Sector rules") {
            scenario("FHIR pharmacy code is rejected for LDAP") {
                val f = preflight(pharmacy.copy(specialization = listOf("offizin-apotheke"))).single { it.attribute == "specialization" }
                f.severity shouldBe FindingSeverity.ERROR
                f.allowedValues!! shouldContain "10 Offizin-Apotheke"
            }
            scenario("Missing pharmacy specialization is ERROR, deliberately empty is WARNING") {
                errors(preflight(pharmacy.copy(specialization = null))) shouldBe listOf("specialization")
                warnings(preflight(pharmacy.copy(specialization = null), setOf("specialization"))) shouldBe listOf("specialization")
            }
            scenario("Missing pharmacy address names sector and table") {
                val f = preflight(pharmacy.copy(streetAddress = null)).single { it.attribute == "streetAddress" }
                f.message shouldContainText "Apotheken"
                f.message shouldContainText "Tabelle 4"
            }
            scenario("KV practice needs domainID, accepts urn:psc") {
                val kv = pharmacy.copy(telematikID = "1-2012345678", specialization = listOf("urn:psc:1.3.6.1.4.1.19376.3.276.1.5.4:ALLG"))
                errors(preflight(kv)) shouldBe listOf("domainID")
            }
            scenario("KV practice with pharmacy code is a format error") {
                val kv = pharmacy.copy(telematikID = "1-2012345678", domainID = listOf("123456789"))
                errors(preflight(kv)) shouldBe listOf("specialization")
            }
            scenario("HBA urn:as resolved and unresolved") {
                val hba = BaseDirectoryEntry("1-1123456789", displayName = "Fit, Heinrich", specialization = listOf("urn:as:1.2.276.0.76.5.514:011001"))
                preflight(hba) shouldBe emptyList()
                warnings(preflight(hba.copy(specialization = listOf("urn:as:1.2.276.0.76.5.514:999999")))) shouldBe listOf("specialization")
            }
        }
    })
