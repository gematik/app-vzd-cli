package de.gematik.ti.directory.elaborate.validation

import de.gematik.ti.directory.elaborate.ElaborateBaseDirectoryEntry
import de.gematik.ti.directory.validation.FindingSeverity
import de.gematik.ti.directory.validation.ValidationRule

object InvalidProfessionOID : ValidationRule<ElaborateBaseDirectoryEntry>({
    it.professionOID?.forEachIndexed { index, elaborateProfessionOID ->
        // if code and display are the same, then it was not resolved using CodeSystems
        if (elaborateProfessionOID.code == elaborateProfessionOID.display) {
            addFinding(ElaborateBaseDirectoryEntry::professionOID, FindingSeverity.ERROR, index = index)
        }
    }
})
