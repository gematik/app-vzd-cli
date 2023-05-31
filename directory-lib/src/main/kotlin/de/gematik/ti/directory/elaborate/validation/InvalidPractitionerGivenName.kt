package de.gematik.ti.directory.elaborate.validation

import de.gematik.ti.directory.elaborate.DirectoryEntryResourceType
import de.gematik.ti.directory.elaborate.ElaborateBaseDirectoryEntry
import de.gematik.ti.directory.validation.FindingSeverity
import de.gematik.ti.directory.validation.ValidationRule

object InvalidPractitionerGivenName : ValidationRule<ElaborateBaseDirectoryEntry>({
    if (it.fhirResourceType == DirectoryEntryResourceType.Practitioner &&
        (it.givenName == null || it.givenName?.trim() == "" || it.givenName == "-")) {
        addFinding(ElaborateBaseDirectoryEntry::givenName, FindingSeverity.ERROR)
    }
})
