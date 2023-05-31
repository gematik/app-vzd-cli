package de.gematik.ti.directory.elaborate.validation

import de.gematik.ti.directory.elaborate.DirectoryEntryResourceType
import de.gematik.ti.directory.elaborate.ElaborateBaseDirectoryEntry
import de.gematik.ti.directory.validation.FindingSeverity
import de.gematik.ti.directory.validation.ValidationRule

object InvalidPractitionerSurname : ValidationRule<ElaborateBaseDirectoryEntry>({
    if (it.fhirResourceType == DirectoryEntryResourceType.Practitioner &&
        (it.sn == null || it.sn?.trim() == "" || it.sn == "-")) {
        addFinding(ElaborateBaseDirectoryEntry::sn, FindingSeverity.ERROR)
    }
})
