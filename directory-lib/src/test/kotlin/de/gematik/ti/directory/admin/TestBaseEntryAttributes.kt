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
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FeatureSpec
import io.kotest.matchers.shouldBe

class TestBaseEntryAttributes :
    FeatureSpec({
        feature("setAttributes") {
            scenario("String, Int, List and Boolean") {
                val b = BaseDirectoryEntry("x")
                setAttributes(b, mapOf("displayName" to "A", "maxKOMLEadr" to "3", "specialization" to "10, 40", "active" to "false"))
                b.displayName shouldBe "A"
                b.maxKOMLEadr shouldBe 3
                b.specialization shouldBe listOf("10", "40")
                b.active shouldBe false
            }
            scenario("Unknown attribute") {
                shouldThrow<DirectoryException> { setAttributes(BaseDirectoryEntry("x"), mapOf("nope" to "1")) }
            }
        }
        feature("applyOverrides") {
            scenario("Blank value clears and is reported") {
                val b = BaseDirectoryEntry("x", specialization = listOf("10"), stateOrProvinceName = "Bayern")
                applyOverrides(b, mapOf("specialization" to "", "stateOrProvinceName" to "", "displayName" to "B")) shouldBe
                    setOf("specialization", "stateOrProvinceName")
                b.specialization shouldBe null
                b.stateOrProvinceName shouldBe null
                b.displayName shouldBe "B"
            }
        }
    })
