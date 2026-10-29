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

import com.github.ajalt.clikt.core.CliktError
import de.gematik.ti.directory.cli.admin.checkCertificateAgainstEntry
import de.gematik.ti.directory.pki.TestCertificates
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FeatureSpec
import io.kotest.engine.spec.tempdir
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

class TestAddCertPreflight :
    FeatureSpec({
        val cert = TestCertificates.load(TestCertificates.smcbRsaEnc)
        val telematikID = cert.certificateInfo.admissionStatement.registrationNumber

        feature("checkCertificateAgainstEntry") {
            scenario("entryType mismatch is explained") {
                val message = checkCertificateAgainstEntry(cert, BaseDirectoryEntry(telematikID, entryType = listOf("4")))!!
                message shouldContain "entryType=3"
                message shouldContain "Betriebsstätte Arzt"
            }
            scenario("matching entry passes") {
                checkCertificateAgainstEntry(cert, BaseDirectoryEntry(telematikID, entryType = listOf("3"))) shouldBe null
            }
            scenario("entry without entryType passes") {
                checkCertificateAgainstEntry(cert, BaseDirectoryEntry(telematikID)) shouldBe null
            }
            scenario("telematikID mismatch") {
                checkCertificateAgainstEntry(cert, BaseDirectoryEntry("3-other"))!! shouldContain "3-other"
            }
        }
        feature("add-cert") {
            scenario("AUT file fails before any request") {
                val autFile = TestCertificates.writeDer(TestCertificates.smcbAut(), tempdir().toPath(), "aut.der")
                shouldThrow<CliktError> { runCLI(listOf("admin", "ru", "add-cert", autFile.toString())) }.message shouldContain "C.HCI.AUT"
            }
        }
    })
