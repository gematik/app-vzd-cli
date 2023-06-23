package de.gematik.ti.directory.elaborate

import de.gematik.ti.directory.admin.BaseDirectoryEntry
import de.gematik.ti.directory.fhir.SimpleValueSet
import de.gematik.ti.directory.fhir.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.decodeFromString

fun BaseDirectoryEntry.infereKind(): TelematikIDMapping {
    return TelematikIDMappings.instance.mapping.firstOrNull {
        it.matches(this)
    } ?: TelematikIDMappings.instance.mapping.last()
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
    val display: String? = null,
) {
    // DOT_MATCHES_ALL required to match the TelematikID with special characters like \n
    @Transient
    val regex = Regex(pattern, setOf(RegexOption.DOT_MATCHES_ALL))
    fun matches(baseDirectoryEntry: BaseDirectoryEntry): Boolean {
        return baseDirectoryEntry.telematikID.matches(regex)
    }
}

@Serializable
data class TelematikIDMappings(
    val mapping: List<TelematikIDMapping>,
) {
    companion object {
        val instance: TelematikIDMappings by lazy {
            json.decodeFromString(SimpleValueSet::class.java.getResource("/mappings/TelematikID.mapping.json")!!.readText())
        }
    }
}
