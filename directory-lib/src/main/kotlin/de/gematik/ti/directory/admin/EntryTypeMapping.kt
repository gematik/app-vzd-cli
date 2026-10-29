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

package de.gematik.ti.directory.admin

import de.gematik.ti.directory.fhir.OrganizationProfessionOID
import de.gematik.ti.directory.fhir.PractitionerProfessionOID
import de.gematik.ti.directory.fhir.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString

private const val PROFESSION_OID_PREFIX = "1.2.276.0.76.4."

@Serializable
data class EntryTypeMappingEntry(
    val entryType: Int,
    val name: String,
    val professionOIDs: List<String>,
)

@Serializable
private data class EntryTypeMappingFile(
    val mapping: List<EntryTypeMappingEntry>,
)

/**
 * gemSpec_VZD Tab_VZD_Mapping_Eintragstyp_und_ProfessionOID. The resource stores the OID suffixes below
 * 1.2.276.0.76.4 so the table can be maintained without touching code.
 */
object EntryTypeMapping {
    val entries: List<EntryTypeMappingEntry> by lazy {
        val file: EntryTypeMappingFile =
            json.decodeFromString(EntryTypeMapping::class.java.getResource("/mappings/EntryType.mapping.json")!!.readText())
        file.mapping.map { entry -> entry.copy(professionOIDs = entry.professionOIDs.map { PROFESSION_OID_PREFIX + it }) }
    }

    private val byOid: Map<String, Int> by lazy {
        entries.flatMap { entry -> entry.professionOIDs.map { it to entry.entryType } }.toMap()
    }

    fun entryTypeOf(professionOID: String): Int? = byOid[professionOID]

    fun entryTypeOf(professionOIDs: List<String>): Int? {
        val known = professionOIDs.mapNotNull { entryTypeOf(it) }.distinct()
        return known.singleOrNull()
    }

    fun nameOf(entryType: Int): String? = entries.firstOrNull { it.entryType == entryType }?.name

    fun describe(professionOID: String): String {
        val display =
            OrganizationProfessionOID.resolveCode(professionOID)?.display
                ?: PractitionerProfessionOID.resolveCode(professionOID)?.display
        return if (display != null) "$professionOID ($display)" else professionOID
    }
}
