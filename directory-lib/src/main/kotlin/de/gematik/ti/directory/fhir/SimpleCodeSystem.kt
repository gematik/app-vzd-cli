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

package de.gematik.ti.directory.fhir

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString

@Serializable
class SimpleConcept(
    val code: String,
    val display: String,
)

@Serializable
data class Coding(
    val code: String,
    val display: String,
    val system: String? = null,
)

@Serializable
class SimpleCodeSystem(
    val name: String,
    val url: String,
    val concept: List<SimpleConcept>,
) {
    fun resolveCode(code: String): Coding? =
        concept.firstOrNull { it.code == code }?.let {
            Coding(code, it.display, url)
        }
}

private fun loadSimpleCodeSystem(name: String): SimpleCodeSystem = json.decodeFromString(SimpleCodeSystem::class.java.getResource("/de.gematik.fhir.directory/CodeSystem-$name.json")!!.readText())

val OrganizationProfessionOID = loadSimpleCodeSystem("OrganizationProfessionOID")
val PractitionerProfessionOID = loadSimpleCodeSystem("PractitionerProfessionOID")
val PharmacyHealthcareSpecialtyCS = loadSimpleCodeSystem("PharmacyHealthcareSpecialtyCS")
val PharmacyTypeLDAPCS = loadSimpleCodeSystem("PharmacyTypeLDAPCS")
val Region = loadSimpleCodeSystem("Region")
val Holder = loadSimpleCodeSystem("HolderCS")
