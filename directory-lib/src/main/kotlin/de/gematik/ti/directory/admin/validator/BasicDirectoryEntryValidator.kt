package de.gematik.ti.directory.admin.validator

import de.gematik.ti.directory.admin.BaseDirectoryEntry
import de.gematik.ti.directory.admin.DirectoryEntry
import io.konform.validation.ValidationBuilder
import io.konform.validation.jsonschema.pattern
import io.konform.validation.onEach

private val telematikIDRegex = Regex("^[0-9]+-[._\\-\\p{L}0-9]+$")

fun ValidationBuilder<DirectoryEntry>.basicallyValid() {
    DirectoryEntry::directoryEntryBase {
        BaseDirectoryEntry::telematikID {
            pattern(telematikIDRegex) hint "Invalid TelematikID format"
        }
        BaseDirectoryEntry::specialization ifPresent {
            onEach {
            }
        }
    }
}
