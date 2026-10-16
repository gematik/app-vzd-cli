/*
Copyright 2026, gematik GmbH

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

*******

For additional notes and disclaimer from gematik and in case of changes
by gematik, find details in the "Readme" file.
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
