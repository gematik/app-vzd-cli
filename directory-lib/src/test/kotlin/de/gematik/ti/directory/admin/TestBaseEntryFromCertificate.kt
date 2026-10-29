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

class TestBaseEntryFromCertificate :
    FeatureSpec({
        feature("Base entry derived from certificate subject") {
            scenario("SMC-B") {
                val b = BaseDirectoryEntry.fromCertificate(TestCertificates.load(TestCertificates.smcbRsaEnc))
                b.telematikID shouldBe "1-SMC-B-Testkarte-883110000117035"
                b.displayName shouldBe "Praxis Lilo Gräfin de BoerTEST-ONLY"
                b.streetAddress shouldBe "Am Markt 1"
                b.postalCode shouldBe "25832"
                b.localityName shouldBe "Tönning"
                b.countryCode shouldBe "DE"
                b.entryType shouldBe null
                b.professionOID shouldBe null
                b.cn shouldBe null
                b.sn shouldBe null
                b.givenName shouldBe null
            }
            scenario("HBA") {
                val b = BaseDirectoryEntry.fromCertificate(TestCertificates.load(TestCertificates.hbaEnc))
                b.telematikID shouldBe "4-11a4103-001"
                b.displayName shouldBe "Fit, Heinrich"
                b.streetAddress shouldBe null
                b.countryCode shouldBe null
            }
            scenario("Synthetic pharmacy with address") {
                val b = BaseDirectoryEntry.fromCertificate(TestCertificates.toDer(TestCertificates.pharmacyEnc()))
                b.displayName shouldBe "Linden Apotheke TEST"
                b.streetAddress shouldBe "Hauptstr. 1"
                b.postalCode shouldBe "10117"
                b.localityName shouldBe "Berlin"
                b.countryCode shouldBe "DE"
            }
        }
    })
