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

import de.gematik.ti.directory.DirectoryException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FeatureSpec
import io.kotest.engine.spec.tempdir
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.bouncycastle.asn1.x509.KeyUsage
import org.bouncycastle.util.encoders.Base64
import kotlin.io.path.readBytes
import kotlin.io.path.writeBytes
import kotlin.io.path.writeText

class TestCertificateSource :
    FeatureSpec({
        val tmp = tempdir().toPath()
        val enc = TestCertificates.synthetic("CN=ENC", "1.2.276.0.76.4.76", KeyUsage.keyAgreement, "3-SMC-B-Test")
        val aut = TestCertificates.synthetic("CN=AUT", "1.2.276.0.76.4.77", KeyUsage.digitalSignature, "3-SMC-B-Test")
        val p12 = tmp.resolve("card.p12")
        p12.writeBytes(TestCertificates.pkcs12("secret".toCharArray(), listOf(enc), listOf(aut.first)))

        feature("Load certificates") {
            scenario("DER") {
                loadCertificates(TestCertificates.smcbRsaEnc) { null }.size shouldBe 1
            }
            scenario("PEM") {
                val b64 = Base64.toBase64String(TestCertificates.smcbRsaEnc.readBytes()).chunked(64).joinToString("\n")
                val pem = tmp.resolve("c.pem")
                pem.writeText("-----BEGIN CERTIFICATE-----\n$b64\n-----END CERTIFICATE-----\n")
                loadCertificates(pem) { null }.first().certificateInfo.admissionStatement.registrationNumber shouldBe
                    "1-SMC-B-Testkarte-883110000117035"
            }
            scenario("PKCS#12 with ENC key entry and AUT trusted cert returns both end-entity certs") {
                val certs = loadCertificates(p12) { "secret".toCharArray() }
                certs.map { it.certificateInfo.subjectInfo.cn }.toSet() shouldBe setOf("ENC", "AUT")
            }
            scenario("PKCS#12 without password") {
                shouldThrow<CertificatePasswordRequired> { loadCertificates(p12) { null } }
            }
            scenario("PKCS#12 wrong password names the file") {
                shouldThrow<DirectoryException> { loadCertificates(p12) { "nope".toCharArray() } }.message shouldContain "card.p12"
            }
            scenario("Format detection") {
                detectCertificateFileFormat(p12.readBytes()) shouldBe CertificateFileFormat.PKCS12
                detectCertificateFileFormat(TestCertificates.smcbRsaEnc.readBytes()) shouldBe CertificateFileFormat.DER
                detectCertificateFileFormat("-----BEGIN CERTIFICATE-----\n".toByteArray()) shouldBe CertificateFileFormat.PEM
            }
        }
    })
