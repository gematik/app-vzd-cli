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

class TestValidation : FeatureSpec({
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
