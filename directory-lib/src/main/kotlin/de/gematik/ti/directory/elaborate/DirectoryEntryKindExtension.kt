package de.gematik.ti.directory.elaborate

import de.gematik.ti.directory.admin.BaseDirectoryEntry
import de.gematik.ti.directory.admin.DirectoryEntry

fun BaseDirectoryEntry.infereKind(): DirectoryEntryKind {
    return DirectoryEntryKind.values().first {
        it.matcher.invoke(this)
    }
}

enum class DirectoryEntryResourceType {
    Organization,
    Practitioner
}

// ktlint-disable enum-entry-name-case
enum class DirectoryEntryKind(val fhirResourceType: DirectoryEntryResourceType, val matcher: (BaseDirectoryEntry) -> Boolean) {
    Arzt(DirectoryEntryResourceType.Practitioner, { baseDirectoryEntry ->
        baseDirectoryEntry.telematikID.let {
            it.startsWith("1-1") ||
                (it.startsWith("1-") && baseDirectoryEntry.personalEntry == true)
        }
    }),
    Arztpraxis(DirectoryEntryResourceType.Organization, { baseDirectoryEntry ->
        baseDirectoryEntry.telematikID.let {
            it.startsWith("1-2") ||
                (it.startsWith("1-") && baseDirectoryEntry.personalEntry == false) ||
                (it.startsWith("4-") && baseDirectoryEntry.personalEntry == false)
        }
    }),
    Zahnarzt(DirectoryEntryResourceType.Practitioner, { baseDirectoryEntry -> baseDirectoryEntry.telematikID.matches("^2-0?1.*".toRegex()) }),
    Zahnarztpraxis(DirectoryEntryResourceType.Organization, { baseDirectoryEntry ->
        baseDirectoryEntry.telematikID.let {
            it.startsWith("2-2") ||
                (it.startsWith("2-") && baseDirectoryEntry.personalEntry == false)
        }
    }),
    Apotheke(DirectoryEntryResourceType.Organization, { baseDirectoryEntry ->
        baseDirectoryEntry.telematikID.let {
            it.matches("^3-...2.*".toRegex()) ||
                (it.startsWith("3-") && baseDirectoryEntry.personalEntry == false)
        }
    }),
    Apotheker(DirectoryEntryResourceType.Practitioner, { baseDirectoryEntry -> baseDirectoryEntry.telematikID.startsWith("3-") }),
    Psychotherapeut(DirectoryEntryResourceType.Practitioner, { baseDirectoryEntry ->
        baseDirectoryEntry.telematikID.let {
            it.startsWith("4-") && baseDirectoryEntry.personalEntry == true
        }
    }),
    Krankenhaus(DirectoryEntryResourceType.Organization, { baseDirectoryEntry -> baseDirectoryEntry.telematikID.startsWith("5-") }),
    Krankenkasse(DirectoryEntryResourceType.Organization, { baseDirectoryEntry -> baseDirectoryEntry.telematikID.startsWith("8-01") }),

    Krankenkasse_ePA(DirectoryEntryResourceType.Organization, { baseDirectoryEntry -> baseDirectoryEntry.telematikID.startsWith("8-03") }),

    HBAGematik(DirectoryEntryResourceType.Practitioner, { baseDirectoryEntry ->
        baseDirectoryEntry.telematikID.let {
            it.startsWith("9-1")
        }
    }),
    SMCBGematik(DirectoryEntryResourceType.Organization, { baseDirectoryEntry ->
        baseDirectoryEntry.telematikID.let {
            it.startsWith("9-2")
        }
    }),
    HBAeGBR(DirectoryEntryResourceType.Practitioner, { baseDirectoryEntry ->
        baseDirectoryEntry.telematikID.let {
            it.startsWith("10-67.23")
        }
    }),
    SMCBeGBR(DirectoryEntryResourceType.Organization, { baseDirectoryEntry ->
        baseDirectoryEntry.telematikID.let {
            it.startsWith("10-67.24")
        }
    }),

    Weitere(DirectoryEntryResourceType.Organization, { _ -> true }),
}
