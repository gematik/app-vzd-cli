package de.gematik.ti.directory.elaborate

import de.gematik.ti.directory.admin.BaseDirectoryEntry
import de.gematik.ti.directory.fhir.SimpleValueSet
import de.gematik.ti.directory.fhir.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.decodeFromString

fun BaseDirectoryEntry.infereKind(): TelematikIDMapping {
    return TelematikIDMappings.instance.mapping.first {
        it.matches(this)
    }
}

enum class DirectoryEntryResourceType {
    Organization,
    Practitioner,
}

@Serializable
data class TelematikIDMapping(
    val pattern: String,
    val fhirResourceType: DirectoryEntryResourceType,
    val code: String,
    val displayShort: String,
    val display: String? = null
) {
    @Transient
    val regex = Regex(pattern)
    fun matches(baseDirectoryEntry: BaseDirectoryEntry): Boolean {
        return baseDirectoryEntry.telematikID.matches(regex)
    }
}

@Serializable
data class TelematikIDMappings(
    val mapping: List<TelematikIDMapping>
) {
    companion object {
        val instance: TelematikIDMappings by lazy {
            json.decodeFromString(SimpleValueSet::class.java.getResource("/mappings/TelematikID.mapping.json")!!.readText())
        }
    }
}

// ktlint-disable enum-entry-name-case
enum class DirectoryEntryKind2(val fhirResourceType: DirectoryEntryResourceType, val matcher: (BaseDirectoryEntry) -> Boolean) {
    Arzt(DirectoryEntryResourceType.Practitioner, { baseDirectoryEntry ->
        baseDirectoryEntry.telematikID.let {
            it.startsWith("1-1")
        }
    }),
    Arztpraxis(DirectoryEntryResourceType.Organization, { baseDirectoryEntry ->
        baseDirectoryEntry.telematikID.let {
            it.startsWith("1-20")
        }
    }),
    Zahnarzt(DirectoryEntryResourceType.Practitioner, { baseDirectoryEntry -> baseDirectoryEntry.telematikID.matches("^2-0?1.*".toRegex()) }),
    Zahnarztpraxis(DirectoryEntryResourceType.Organization, { baseDirectoryEntry ->
        baseDirectoryEntry.telematikID.startsWith("2-2")
    }),
    Apotheke(DirectoryEntryResourceType.Organization, { baseDirectoryEntry ->
        baseDirectoryEntry.telematikID.matches("^3-...2.*".toRegex())
    }),
    Apotheker(DirectoryEntryResourceType.Practitioner, { baseDirectoryEntry -> baseDirectoryEntry.telematikID.startsWith("3-") }),
    Psychotherapeut(DirectoryEntryResourceType.Practitioner, { baseDirectoryEntry ->
        baseDirectoryEntry.telematikID.startsWith("4-")
    }),
    Krankenhaus(DirectoryEntryResourceType.Organization, { baseDirectoryEntry -> baseDirectoryEntry.telematikID.startsWith("5-") }),
    Krankenkasse(DirectoryEntryResourceType.Organization, { baseDirectoryEntry -> baseDirectoryEntry.telematikID.startsWith("8-01") }),

    Krankenkasse_ePA(DirectoryEntryResourceType.Organization, { baseDirectoryEntry -> baseDirectoryEntry.telematikID.startsWith("8-03") }),

    HBAGematik(DirectoryEntryResourceType.Practitioner, { baseDirectoryEntry ->
        baseDirectoryEntry.telematikID.startsWith("9-1")
    }),
    SMCBGematik(DirectoryEntryResourceType.Organization, { baseDirectoryEntry ->
        baseDirectoryEntry.telematikID.startsWith("9-2")
    }),
    HBAeGBR(DirectoryEntryResourceType.Practitioner, { baseDirectoryEntry ->
        baseDirectoryEntry.telematikID.startsWith("10-67.23")
    }),
    SMCBeGBR(DirectoryEntryResourceType.Organization, { baseDirectoryEntry ->
        baseDirectoryEntry.telematikID.startsWith("10-67.24")
    }),

    Weitere(DirectoryEntryResourceType.Organization, { _ -> true }),
}
