package de.gematik.ti.directory.validator.gemval

import de.gematik.ti.directory.elaborate.DirectoryEntryKind
import de.gematik.ti.directory.elaborate.ElaborateBaseDirectoryEntry
import de.gematik.ti.directory.elaborate.ElaborateDirectoryEntry
import de.gematik.ti.directory.validator.ElaborateDirectoryEntryValidation
import io.kotest.core.spec.style.FeatureSpec
import io.kotest.matchers.shouldBe

class TestValidator : FeatureSpec({
    feature("Mini-Validator") {
        scenario("Test Mini-Validator DSL") {
            val validationResult = ElaborateDirectoryEntryValidation.validate(
                ElaborateDirectoryEntry(
                    kind = DirectoryEntryKind.Arzt,
                    base = ElaborateBaseDirectoryEntry(
                        telematikID="invalid-telematik-id",
                        displayName="-",
                        active = true)
                )
            )

            println(validationResult)
            validationResult.isValid() shouldBe false
        }
    }
})