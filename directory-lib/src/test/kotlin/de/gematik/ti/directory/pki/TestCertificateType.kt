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

package de.gematik.ti.directory.pki

import io.kotest.core.spec.style.FeatureSpec
import io.kotest.matchers.shouldBe
import org.bouncycastle.asn1.x509.KeyUsage

class TestCertificateType :
    FeatureSpec({
        feature("Certificate type from policy OIDs") {
            scenario("SMC-B RSA ENC") {
                val cert = TestCertificates.load(TestCertificates.smcbRsaEnc).certificate
                cert.certificateType() shouldBe CertificateType.C_HCI_ENC
                cert.isEncryptionCertificate() shouldBe true
                cert.hasAdmission() shouldBe true
            }
            scenario("SMC-B ECC ENC with keyAgreement only") {
                TestCertificates.load(TestCertificates.smcbEccEnc).certificate.isEncryptionCertificate() shouldBe true
            }
            scenario("HBA ENC") {
                TestCertificates.load(TestCertificates.hbaEnc).certificate.certificateType() shouldBe CertificateType.C_HP_ENC
            }
            scenario("Synthetic AUT is not an encryption certificate") {
                val (cert, _) = TestCertificates.synthetic("CN=AUT", "1.2.276.0.76.4.77", KeyUsage.digitalSignature, "3-SMC-B-Test")
                cert.certificateType() shouldBe CertificateType.C_HCI_AUT
                cert.isEncryptionCertificate() shouldBe false
            }
            scenario("Unknown policy") {
                val (cert, _) = TestCertificates.synthetic("CN=X", "1.2.3.4", KeyUsage.keyAgreement, null)
                cert.certificateType() shouldBe null
                cert.hasAdmission() shouldBe false
            }
        }
    })
