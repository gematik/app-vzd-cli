package de.gematik.ti.directory.validator.rules

import de.gematik.ti.directory.validator.Rule
import de.gematik.ti.directory.validator.ValidationBuilder
import de.gematik.ti.directory.validator.ValidationSeverity

fun ValidationBuilder<String?>.invalidDisplayName() = addRule(
    Rule(
        code="InvalidDisplayName",
        severity= ValidationSeverity.ERROR) {
        if (it?.trim() == "-") {
            false
        } else {
            it?.matches(Regex("^[0-9]*\$")) != true
        }
    }
)