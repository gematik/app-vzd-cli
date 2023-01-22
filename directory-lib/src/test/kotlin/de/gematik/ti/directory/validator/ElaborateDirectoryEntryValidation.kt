package de.gematik.ti.directory.validator

import de.gematik.ti.directory.elaborate.ElaborateBaseDirectoryEntry
import de.gematik.ti.directory.elaborate.ElaborateDirectoryEntry
import de.gematik.ti.directory.validator.rules.invalidDisplayName

val ElaborateDirectoryEntryValidation = Validation {
    ElaborateDirectoryEntry::base {
        ElaborateBaseDirectoryEntry::displayName {
            invalidDisplayName()
        }
    }
}