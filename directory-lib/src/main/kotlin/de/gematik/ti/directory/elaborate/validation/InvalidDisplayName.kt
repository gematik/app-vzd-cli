package de.gematik.ti.directory.elaborate.validation

import de.gematik.ti.directory.elaborate.ElaborateBaseDirectoryEntry
import de.gematik.ti.directory.validation.FindingSeverity
import de.gematik.ti.directory.validation.ValidationRule

object InvalidDisplayName: ValidationRule<ElaborateBaseDirectoryEntry>({
    if (it.displayName?.trim() == "-") {
        addFinding(ElaborateBaseDirectoryEntry::displayName, FindingSeverity.ERROR)
    } else if (it.displayName?.matches(Regex("^[0-9]*\$")) == true) {
        addFinding(ElaborateBaseDirectoryEntry::displayName, FindingSeverity.ERROR)
    }
})
