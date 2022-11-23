package de.gematik.ti.directory.validator

import de.gematik.ti.directory.admin.BaseDirectoryEntry
import de.gematik.ti.directory.admin.DirectoryEntry
import io.kotest.core.spec.style.FeatureSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.valiktor.ConstraintViolationException
import org.valiktor.functions.isNotNull
import org.valiktor.validate

class TestValidator: FeatureSpec({
    feature("TelematikID") {
        scenario("TelematikID Regex") {
            val pattern = Regex("^[0-9]+-[._\\-\\p{L}0-9]+$")
            "1-1".matches(pattern) shouldBe true
            "11-1".matches(pattern) shouldBe true
            "a-1".matches(pattern) shouldBe false
            "1-123.a_b-cüäß".matches(pattern) shouldBe true
            "1-123.456\n".matches(pattern) shouldBe false
        }
    }
    feature("Validator Spec") {
        val spec = IdentifierValidators()
        val entry1 = DirectoryEntry(BaseDirectoryEntry("1-abc"))
        val findings1 = spec.runWith(entry1)
        findings1.find { it.type == FindingType.TELEMATIK_ID_INVALID_FORMAT } shouldBe null
        val entry2 = DirectoryEntry(BaseDirectoryEntry("1-abc\n"))
        val findings2 = spec.runWith(entry2)
        println(findings2)
        findings2.find { it.type == FindingType.TELEMATIK_ID_INVALID_FORMAT } shouldNotBe null
        findings2.find { it.type == FindingType.HOLDER_IS_EMPTY } shouldNotBe null
    }

    feature("Validate using valiktor") {
        scenario("Empty") {
            val entry2 = DirectoryEntry(BaseDirectoryEntry("1-abc\n"))
            try {
                validate(entry2.directoryEntryBase) {
                    validate(BaseDirectoryEntry::maxKOMLEadr).isNotNull()
                }
            } catch (ex: ConstraintViolationException) {
                ex.constraintViolations
                    .map { "${it.property}: ${it.constraint.name}" }
                    .forEach(::println)
            }
        }
    }
})