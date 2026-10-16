/*
Copyright 2026, gematik GmbH

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

*******

For additional notes and disclaimer from gematik and in case of changes
by gematik, find details in the "Readme" file.
*/

package de.gematik.ti.directory.elaborate

import de.gematik.ti.directory.admin.BaseDirectoryEntry
import de.gematik.ti.directory.fhir.SimpleValueSet
import de.gematik.ti.directory.fhir.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.decodeFromString

fun BaseDirectoryEntry.infereKind(): TelematikIDMapping =
    TelematikIDMappings.instance.mapping.firstOrNull {
        it.matches(this)
    } ?: TelematikIDMappings.instance.mapping.last()

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

    fun matches(baseDirectoryEntry: BaseDirectoryEntry): Boolean = baseDirectoryEntry.telematikID.matches(regex)
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
