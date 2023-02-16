package de.gematik.ti.directory.elaborate.validation

import de.gematik.ti.directory.elaborate.ElaborateBaseDirectoryEntry
import de.gematik.ti.directory.validation.FindingSeverity
import de.gematik.ti.directory.validation.ValidationRule
import java.util.Locale

object InvalidCountryCode : ValidationRule<ElaborateBaseDirectoryEntry>({
    if (!Locale.getISOCountries().contains(it.countryCode?.uppercase())) {
        addFinding(ElaborateBaseDirectoryEntry::countryCode, FindingSeverity.ERROR)
    }
})
