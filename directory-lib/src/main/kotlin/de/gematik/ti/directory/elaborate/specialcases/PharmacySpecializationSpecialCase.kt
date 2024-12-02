package de.gematik.ti.directory.elaborate.specialcases

import de.gematik.ti.directory.elaborate.ElaborateDirectoryEntry
import de.gematik.ti.directory.elaborate.SpecialCase
import de.gematik.ti.directory.fhir.PharmacyHealthcareSpecialtyCS

class PharmacySpecializationSpecialCase : SpecialCase {
    override fun apply(entry: ElaborateDirectoryEntry) {
        if (entry.kind == "Apotheke") {
            entry.base.specialization =
                entry.base.specialization?.map {
                    if (it.system != null) {
                        it
                    } else {
                        PharmacyHealthcareSpecialtyCS.resolveCode(it.code) ?: it
                    }
                }
        }
    }
}
