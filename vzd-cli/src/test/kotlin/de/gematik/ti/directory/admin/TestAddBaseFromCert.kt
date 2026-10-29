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
import com.github.ajalt.clikt.core.UsageError
import de.gematik.ti.directory.cli.admin.formatFindingsForAbort
import de.gematik.ti.directory.cli.admin.promptForErrors
import de.gematik.ti.directory.pki.TestCertificates
import de.gematik.ti.directory.admin.withAnswers
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FeatureSpec
import io.kotest.engine.spec.tempdir
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import kotlin.io.path.writeText

class TestAddBaseFromCert :
    FeatureSpec({
        val dir = tempdir().toPath()
        val pharmacyCert = TestCertificates.pharmacyEnc()
        val pharmacy = TestCertificates.writeDer(pharmacyCert, dir, "pharmacy.der")
        val pharmacyDraft = draftFromCertificates(listOf(TestCertificates.toDer(pharmacyCert)), emptyMap())

        feature("add-base --from-cert aborts before any request") {
            scenario("--from-cert together with file argument is a usage error") {
                val entryFile = dir.resolve("entry.yaml").also { it.writeText("telematikID: x\n") }
                shouldThrow<UsageError> {
                    runCLI(listOf("admin", "ru", "add-base", "--from-cert", pharmacy.toString(), entryFile.toString()))
                }.message shouldContain "--from-cert"
            }
            scenario("--no-prompt with invalid postalCode names the fix") {
                val e =
                    shouldThrow<CliktError> {
                        runCLI(listOf("admin", "ru", "add-base", "--from-cert", pharmacy.toString(), "--no-prompt", "-s", "specialization=10", "-s", "postalCode=1011"))
                    }
                e.message shouldContain "postalCode must be a 5 digit number"
                e.message shouldContain "-s postalCode="
            }
            scenario("--no-prompt with missing specialization lists the pharmacy codes") {
                val e = shouldThrow<CliktError> { runCLI(listOf("admin", "ru", "add-base", "--from-cert", pharmacy.toString(), "--no-prompt")) }
                e.message shouldContain "10 Offizin-Apotheke"
                e.message shouldContain "Tabelle 4"
            }
            scenario("entryType override contradiction") {
                shouldThrow<CliktError> {
                    runCLI(listOf("admin", "ru", "add-base", "--from-cert", pharmacy.toString(), "--no-prompt", "-s", "entryType=4"))
                }.message shouldContain "entryType 3"
            }
        }

        feature("Prompter") {
            scenario("formatFindingsForAbort lists allowed pharmacy codes and the escape hatch") {
                val text = formatFindingsForAbort(pharmacyDraft)
                text shouldContain "10 Offizin-Apotheke"
                text shouldContain "-s specialization="
                text shouldContain "Tabelle 4"
            }
            scenario("promptForErrors applies answers and treats blank as deliberately empty") {
                val draft = draftFromCertificates(listOf(TestCertificates.toDer(pharmacyCert)), mapOf("postalCode" to "1011"))
                val asked = mutableListOf<String>()
                val defaults = mutableMapOf<String, String?>()
                val result =
                    promptForErrors(draft) { f, d ->
                        asked += f.attribute
                        defaults[f.attribute] = d
                        if (f.attribute == "postalCode") "10117" else ""
                    }
                asked shouldContainExactlyInAnyOrder listOf("postalCode", "specialization")
                defaults["postalCode"] shouldBe "1011"
                result.hasErrors() shouldBe false
                result.base.postalCode shouldBe "10117"
                result.base.specialization shouldBe null
            }
            scenario("formatFindingsForAbort omits the --no-prompt hint for interactive users") {
                formatFindingsForAbort(pharmacyDraft, interactive = true) shouldNotContain "--no-prompt"
                formatFindingsForAbort(pharmacyDraft) shouldContain "--no-prompt"
            }
            scenario("promptForErrors re-asks while the answer is invalid") {
                val draft = draftFromCertificates(listOf(TestCertificates.toDer(pharmacyCert)), mapOf("specialization" to "10"))
                val answers = ArrayDeque(listOf("1011", "abc", "10117"))
                val asked = mutableListOf<String>()
                val result = promptForErrors(draft.withAnswers(mapOf("postalCode" to "1011"))) { f, _ -> asked += f.attribute; answers.removeFirst() }
                asked shouldBe listOf("postalCode", "postalCode", "postalCode")
                result.hasErrors() shouldBe false
                result.base.postalCode shouldBe "10117"
            }
            scenario("promptForErrors gives up after three invalid answers") {
                val draft = draftFromCertificates(listOf(TestCertificates.toDer(pharmacyCert)), mapOf("specialization" to "10", "postalCode" to "1011"))
                val result = promptForErrors(draft) { _, _ -> "1" }
                result.hasErrors() shouldBe true
            }
            scenario("promptForErrors aborts on end of input") {
                val draft = draftFromCertificates(listOf(TestCertificates.toDer(pharmacyCert)), emptyMap())
                shouldThrow<CliktError> { promptForErrors(draft) { _, _ -> null } }.message shouldContain "Aborted"
            }
            scenario("promptForErrors treats the dash answer as deliberately empty") {
                val draft = draftFromCertificates(listOf(TestCertificates.toDer(pharmacyCert)), emptyMap())
                val result = promptForErrors(draft) { _, _ -> "-" }
                result.hasErrors() shouldBe false
                result.base.specialization shouldBe null
            }
        }
    })
