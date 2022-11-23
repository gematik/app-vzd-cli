package de.gematik.ti.directory.validator

import de.gematik.ti.directory.admin.BaseDirectoryEntry
import de.gematik.ti.directory.admin.DirectoryEntry
import de.gematik.ti.directory.cli.escape

class IdentifierValidators: ValidatorSpec({
    validator("Sonderzeichen in telematikID") {
        val pattern = Regex("^[0-9]+-[._\\-\\p{L}0-9]+$")
        if (!entry.directoryEntryBase.telematikID.matches(pattern)) {
            report(FindingType.TELEMATIK_ID_INVALID_FORMAT, "${entry.directoryEntryBase.telematikID.escape()}")
        }
    }
    validator("Leeres holder") {
        if (!(entry.directoryEntryBase.holder?.isNotEmpty() == true))
            report(FindingType.HOLDER_IS_EMPTY)
    }
})