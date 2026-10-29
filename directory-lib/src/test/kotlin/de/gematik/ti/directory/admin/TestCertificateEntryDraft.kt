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

import de.gematik.ti.directory.DirectoryException
import de.gematik.ti.directory.pki.TestCertificates
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FeatureSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain as shouldContainText
import kotlinx.datetime.Instant
import org.bouncycastle.asn1.x509.KeyUsage

class TestCertificateEntryDraft :
    FeatureSpec({
        feature("Certificate selection") {
            scenario("Expired certificates are dropped") {
                shouldThrow<CertificateSelectionException> {
                    draftFromCertificates(listOf(TestCertificates.load(TestCertificates.smcbRsaEnc)), emptyMap(), now = Instant.parse("2030-01-01T00:00:00Z"))
                }.message shouldContainText "expired"
            }
            scenario("Only AUT present") {
                val e = shouldThrow<CertificateSelectionException> { selectEncryptionCertificates(listOf(TestCertificates.toDer(TestCertificates.smcbAut()))) }
                e.message shouldContainText "C.HCI.AUT"
                e.message shouldContainText "C.HCI.ENC"
            }
            scenario("Two TelematikIDs") {
                val a = TestCertificates.synthetic("CN=A", "1.2.276.0.76.4.76", KeyUsage.keyAgreement, "3-A").first
                val b = TestCertificates.synthetic("CN=B", "1.2.276.0.76.4.76", KeyUsage.keyAgreement, "3-B").first
                shouldThrow<CertificateSelectionException> {
                    selectEncryptionCertificates(listOf(a, b).map(TestCertificates::toDer))
                }.message shouldContainText "3-B"
            }
            scenario("RSA and ECC ENC of the same card are both sent, oldest first") {
                val certs = listOf(TestCertificates.smcbEccEnc, TestCertificates.smcbRsaEnc).map(TestCertificates::load)
                val d = draftFromCertificates(certs, emptyMap(), now = TestCertificates.smcbValidAt)
                d.certificates.size shouldBe 2
                d.certificates.zipWithNext().all { (a, b) -> a.certificateInfo.notBefore <= b.certificateInfo.notBefore } shouldBe true
            }
        }

        feature("Draft") {
            scenario("SMC-B DER builds request without derived attributes") {
                val d = draftFromCertificates(listOf(TestCertificates.load(TestCertificates.smcbRsaEnc)), emptyMap(), now = TestCertificates.smcbValidAt)
                val req = d.toCreateDirectoryEntry()
                req.directoryEntryBase!!.entryType shouldBe null
                req.directoryEntryBase!!.professionOID shouldBe null
                req.directoryEntryBase!!.streetAddress shouldBe "Am Markt 1"
                req.userCertificates!!.size shouldBe 1
                req.userCertificates!!.first().userCertificate shouldBe d.certificates.first()
                d.entryType shouldBe 3
            }
            scenario("entryType override must match mapping") {
                shouldThrow<DirectoryException> {
                    draftFromCertificates(listOf(TestCertificates.toDer(TestCertificates.pharmacyEnc())), mapOf("entryType" to "4"))
                }.message shouldContainText "entryType 3"
            }
            scenario("Matching entryType override is accepted but not sent") {
                val d = draftFromCertificates(listOf(TestCertificates.toDer(TestCertificates.pharmacyEnc())), mapOf("entryType" to "3", "specialization" to "10"))
                d.toCreateDirectoryEntry().directoryEntryBase!!.entryType shouldBe null
                d.hasErrors() shouldBe false
            }
            scenario("Deliberately empty specialization yields warning only") {
                val d = draftFromCertificates(listOf(TestCertificates.toDer(TestCertificates.pharmacyEnc())), mapOf("specialization" to ""))
                d.hasErrors() shouldBe false
                d.findings.map { it.attribute } shouldContain "specialization"
                d.toCreateDirectoryEntry().directoryEntryBase!!.specialization shouldBe null
            }
            scenario("withAnswers resolves a finding") {
                val d = draftFromCertificates(listOf(TestCertificates.toDer(TestCertificates.pharmacyEnc())), emptyMap())
                d.hasErrors() shouldBe true
                d.withAnswers(mapOf("specialization" to "10")).hasErrors() shouldBe false
            }
            scenario("withAnswers with blank marks deliberately empty") {
                val d = draftFromCertificates(listOf(TestCertificates.toDer(TestCertificates.pharmacyEnc())), emptyMap())
                val answered = d.withAnswers(mapOf("specialization" to ""))
                answered.hasErrors() shouldBe false
                answered.deliberatelyEmpty shouldContain "specialization"
            }
        }
    })
