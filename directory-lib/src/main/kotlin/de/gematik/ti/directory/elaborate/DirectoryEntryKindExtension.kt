/*
 * Copyright (Change Date see Readme), gematik GmbH
 *
 * Licensed under the EUPL, Version 1.2 or - as soon they will be approved by the
 * European Commission – subsequent versions of the EUPL (the "Licence").
 * You may not use this work except in compliance with the Licence.
 *
 * You find a copy of the Licence in the "Licence" file or at
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either expressed or implied.
 * In case of changes by gematik find details in the "Readme" file.
 *
 * See the Licence for the specific language governing permissions and limitations under the Licence.
 *
 * *******
 *
 * For additional notes and disclaimer from gematik and in case of changes by gematik,
 * find details in the "Readme" file.
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
