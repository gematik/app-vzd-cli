package de.gematik.ti.directory.elaborate.validation

import de.gematik.ti.directory.elaborate.ElaborateBaseDirectoryEntry
import de.gematik.ti.directory.validation.FindingSeverity
import de.gematik.ti.directory.validation.ValidationRule

object InvalidSpecialization : ValidationRule<ElaborateBaseDirectoryEntry>({
    it.specialization?.forEachIndexed { index, elaborateSpecialization ->
        // if code and display are the same, then it was not resolved using CodeSystems
        if (elaborateSpecialization.code == elaborateSpecialization.display) {
            addFinding(ElaborateBaseDirectoryEntry::specialization, FindingSeverity.ERROR, index = index)
        }
    }
})
