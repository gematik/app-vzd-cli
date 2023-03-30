package de.gematik.ti.directory.elaborate

import de.gematik.ti.directory.admin.DirectoryEntry

fun DirectoryEntry.infereKind(): DirectoryEntryKind {
    return DirectoryEntryKind.values().first {
        it.matcher.invoke(this)
    }
}

// ktlint-disable enum-entry-name-case
enum class DirectoryEntryKind(val matcher: (DirectoryEntry) -> Boolean) {
    Arzt({ directoryEntry ->
        directoryEntry.directoryEntryBase.telematikID.let {
            it.startsWith("1-1") ||
                (it.startsWith("1-") && directoryEntry.directoryEntryBase.personalEntry == true)
        }
    }),
    Arztpraxis({ directoryEntry ->
        directoryEntry.directoryEntryBase.telematikID.let {
            it.startsWith("1-2") ||
                (it.startsWith("1-") && directoryEntry.directoryEntryBase.personalEntry == false) ||
                (it.startsWith("4-") && directoryEntry.directoryEntryBase.personalEntry == false)
        }
    }),
    Zahnarzt({ directoryEntry -> directoryEntry.directoryEntryBase.telematikID.matches("^2-0?1.*".toRegex()) }),
    Zahnarztpraxis({ directoryEntry ->
        directoryEntry.directoryEntryBase.telematikID.let {
            it.startsWith("2-2") ||
                (it.startsWith("2-") && directoryEntry.directoryEntryBase.personalEntry == false)
        }
    }),
    Apotheke({ directoryEntry ->
        directoryEntry.directoryEntryBase.telematikID.let {
            it.matches("^3-...2.*".toRegex()) ||
                (it.startsWith("3-") && directoryEntry.directoryEntryBase.personalEntry == false)
        }
    }),
    Apotheker({ directoryEntry -> directoryEntry.directoryEntryBase.telematikID.startsWith("3-") }),
    Psychotherapeut({ directoryEntry ->
        directoryEntry.directoryEntryBase.telematikID.let {
            it.startsWith("4-") && directoryEntry.directoryEntryBase.personalEntry == true
        }
    }),
    Krankenhaus({ directoryEntry -> directoryEntry.directoryEntryBase.telematikID.startsWith("5-") }),
    Krankenkasse({ directoryEntry -> directoryEntry.directoryEntryBase.telematikID.startsWith("8-01") }),

    Krankenkasse_ePA({ directoryEntry -> directoryEntry.directoryEntryBase.telematikID.startsWith("8-03") }),

    HBAGematik({ directoryEntry ->
        directoryEntry.directoryEntryBase.telematikID.let {
            it.startsWith("9-1")
        }
    }),
    SMCBGematik({ directoryEntry ->
        directoryEntry.directoryEntryBase.telematikID.let {
            it.startsWith("9-2")
        }
    }),
    HBAeGBR({ directoryEntry ->
        directoryEntry.directoryEntryBase.telematikID.let {
            it.startsWith("10-67.23")
        }
    }),
    SMCBeGBR({ directoryEntry ->
        directoryEntry.directoryEntryBase.telematikID.let {
            it.startsWith("10-67.24")
        }
    }),

    Weitere({ _ -> true }),
}
