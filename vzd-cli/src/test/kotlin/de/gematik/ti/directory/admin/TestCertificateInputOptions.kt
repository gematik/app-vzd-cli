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

import com.github.ajalt.clikt.core.UsageError
import de.gematik.ti.directory.cli.admin.parsePassin
import de.gematik.ti.directory.pki.TestCertificates
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FeatureSpec
import io.kotest.engine.spec.tempdir
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.bouncycastle.asn1.x509.KeyUsage
import kotlin.io.path.writeBytes

class TestCertificateInputOptions :
    FeatureSpec({
        feature("parsePassin") {
            scenario("pass:") { parsePassin("pass:secret", { null }, { "" }).concatToString() shouldBe "secret" }
            scenario("env:") { parsePassin("env:P", { if (it == "P") "fromenv" else null }, { "" }).concatToString() shouldBe "fromenv" }
            scenario("file: uses first line") { parsePassin("file:x", { null }, { "line1\nline2" }).concatToString() shouldBe "line1" }
            scenario("missing env") { shouldThrow<UsageError> { parsePassin("env:NOPE", { null }, { "" }) } }
            scenario("unknown prefix") { shouldThrow<UsageError> { parsePassin("plain", { null }, { "" }) } }
        }
        feature("cert-info") {
            scenario("prints certificateType and entryType") {
                val out = runCLI(listOf("admin", "cert-info", TestCertificates.smcbRsaEnc.toString()))
                out shouldContain "certificateType: C.HCI.ENC"
                out shouldContain "entryType: 3"
            }
            scenario("reads PKCS#12 with --passin") {
                val enc = TestCertificates.synthetic("CN=ENC", "1.2.276.0.76.4.76", KeyUsage.keyAgreement, "3-SMC-B-Test")
                val p12 = tempdir().toPath().resolve("card.p12")
                p12.writeBytes(TestCertificates.pkcs12("secret".toCharArray(), listOf(enc), listOf(TestCertificates.smcbAut())))
                val out = runCLI(listOf("admin", "cert-info", "--passin", "pass:secret", p12.toString()))
                out shouldContain "certificateType: C.HCI.ENC"
                out shouldContain "certificateType: C.HCI.AUT"
            }
        }
    })
