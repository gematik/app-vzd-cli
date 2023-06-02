package de.gematik.ti.directory.elaborate.validation

import de.gematik.ti.directory.elaborate.ElaborateBaseDirectoryEntry
import de.gematik.ti.directory.validation.FindingSeverity
import de.gematik.ti.directory.validation.ValidationRule

object MissingProfessionOID : ValidationRule<ElaborateBaseDirectoryEntry>({
    if (it.professionOID == null || it.professionOID?.isEmpty() == true) {
        addFinding(ElaborateBaseDirectoryEntry::professionOID, FindingSeverity.ERROR)
    }
})
