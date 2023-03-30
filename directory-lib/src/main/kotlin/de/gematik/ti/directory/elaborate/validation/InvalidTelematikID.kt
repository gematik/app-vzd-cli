package de.gematik.ti.directory.elaborate.validation

import de.gematik.ti.directory.elaborate.ElaborateBaseDirectoryEntry
import de.gematik.ti.directory.validation.FindingSeverity
import de.gematik.ti.directory.validation.ValidationRule

object InvalidTelematikID : ValidationRule<ElaborateBaseDirectoryEntry>({
    val pattern = Regex("^[0-9]+-[._\\-\\p{L}0-9]+$")
    if (!it.telematikID.matches(pattern)) {
        addFinding(ElaborateBaseDirectoryEntry::telematikID, FindingSeverity.ERROR)
    }
})
