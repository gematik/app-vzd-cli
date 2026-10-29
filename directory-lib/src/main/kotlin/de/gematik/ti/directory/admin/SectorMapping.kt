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

import de.gematik.ti.directory.fhir.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.decodeFromString

enum class CardType { HBA, SMC_B }

enum class SpecializationFormat { PHARMACY_LDAP, PSC, AS, NONE }

/**
 * One attribute table of gemILF_Pflege_VZD chapter 4, selected by the TelematikID regex from Tabelle_TID_Mapping.
 * The VZD checks mandatory attributes per sector (rule V009), so the CLI follows the same assignment.
 */
@Serializable
data class Sector(
    val pattern: String,
    val name: String,
    val table: Int,
    val cardType: CardType,
    val mandatory: List<String>,
    val specializationFormat: SpecializationFormat,
) {
    @Transient
    val regex = Regex(pattern, setOf(RegexOption.DOT_MATCHES_ALL))

    fun matches(telematikID: String): Boolean = regex.containsMatchIn(telematikID)
}

@Serializable
private data class SectorMappingFile(
    val sectors: List<Sector>,
)

object SectorMapping {
    val sectors: List<Sector> by lazy {
        json.decodeFromString<SectorMappingFile>(SectorMapping::class.java.getResource("/mappings/Sector.mapping.json")!!.readText()).sectors
    }

    fun sectorOf(telematikID: String): Sector? = sectors.firstOrNull { it.matches(telematikID) }
}
