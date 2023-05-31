package de.gematik.ti.directory.elaborate.validation

import de.gematik.ti.directory.elaborate.ElaborateBaseDirectoryEntry
import de.gematik.ti.directory.validation.Finding
import de.gematik.ti.directory.validation.Validation

fun ElaborateBaseDirectoryEntry.validate(): Map<String, List<Finding>>? {
    return Validation(
        listOf(
            InvalidDisplayName,
            InvalidSpecialization,
            InvalidTelematikID,
            InvalidCountryCode,
            InvalidProfessionOID,
            InvalidPractitionerGivenName,
            InvalidPractitionerSurname,
            MissingProfessionOID,
        ),
        this,
    ).validate()
}
