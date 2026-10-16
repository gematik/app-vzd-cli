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

package de.gematik.ti.directory.validation

import de.gematik.ti.directory.admin.BaseDirectoryEntry
import de.gematik.ti.directory.admin.DirectoryEntry
import de.gematik.ti.directory.elaborate.ElaborateBaseDirectoryEntry
import de.gematik.ti.directory.elaborate.elaborate
import de.gematik.ti.directory.elaborate.validation.validate
import io.kotest.core.spec.style.FeatureSpec
import io.kotest.matchers.shouldNotBe
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class TestValidation :
    FeatureSpec({
        feature("Validation") {
            scenario("Base with Invalid DisplayName") {
                val base = BaseDirectoryEntry(telematikID = "bad-telematik-id", displayName = "-").elaborate()
                val validationResult = base.validate()
                println(Json.encodeToString(validationResult))
                validationResult?.get(ElaborateBaseDirectoryEntry::displayName.name) shouldNotBe null
            }
            scenario("Entry with invalid TelematikID") {
                val entry = DirectoryEntry(BaseDirectoryEntry(telematikID = "bad-telematik-id"))
                val elaboratedEntry = entry.elaborate()
                elaboratedEntry.validationResult?.base shouldNotBe null
                elaboratedEntry.validationResult?.base?.get("telematikID") shouldNotBe null
            }
        }
    })
