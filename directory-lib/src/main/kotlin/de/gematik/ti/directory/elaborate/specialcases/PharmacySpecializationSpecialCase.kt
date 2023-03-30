package de.gematik.ti.directory.elaborate.specialcases

import de.gematik.ti.directory.elaborate.DirectoryEntryKind
import de.gematik.ti.directory.elaborate.ElaborateDirectoryEntry
import de.gematik.ti.directory.elaborate.SpecialCase
import de.gematik.ti.directory.fhir.PharacyTypeCS

class PharmacySpecializationSpecialCase : SpecialCase {
    override fun apply(entry: ElaborateDirectoryEntry) {
        if (entry.kind == DirectoryEntryKind.Apotheke) {
            entry.base.specialization = entry.base.specialization?.map {
                if (it.system != null) {
                    it
                } else {
                    PharacyTypeCS.resolveCode(it.code) ?: it
                }
            }
        }
    }
}
