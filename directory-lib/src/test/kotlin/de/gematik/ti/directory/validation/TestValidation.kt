package de.gematik.ti.directory.validation

import de.gematik.ti.directory.admin.BaseDirectoryEntry
import de.gematik.ti.directory.elaborate.ElaborateBaseDirectoryEntry
import de.gematik.ti.directory.elaborate.elaborate
import de.gematik.ti.directory.validation.rules.InvalidDisplayName
import io.kotest.core.spec.style.FeatureSpec
import io.kotest.matchers.shouldNotBe
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

fun ElaborateBaseDirectoryEntry.validate(): ValidationResult {
    return Validation(listOf(
        InvalidDisplayName
    ), this).validate()
}

class TestValidation : FeatureSpec({
    feature("Validation") {
        scenario("Invalid DisplayName") {
            val base = BaseDirectoryEntry(telematikID = "bad-telematik-id", displayName = "-").elaborate()
            val validationResult = base.validate()
            println(Json.encodeToString(validationResult))
            validationResult.attributes?.get(ElaborateBaseDirectoryEntry::displayName.name) shouldNotBe null
        }
    }
})