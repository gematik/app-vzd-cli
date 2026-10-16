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
data class SimpleValueSet(
    val name: String,
    val compose: ComposeBackboneElement? = null,
) {
    fun resolveCode(
        system: String,
        code: String,
    ): Coding? =
        compose?.include?.firstOrNull { it.system == system }?.concept?.firstOrNull { it.code == code }?.let {
            Coding(it.code, it.display, system)
        }
}

@Serializable
data class ComposeBackboneElement(
    val include: List<SimpleCodesystemReference>,
)

@Serializable
data class SimpleCodesystemReference(
    val system: String,
    val concept: List<SimpleConcept>? = null,
)

private fun loadSimpleValueSet(name: String): SimpleValueSet = json.decodeFromString(SimpleValueSet::class.java.getResource("/de.gematik.fhir.directory/ValueSet-$name.json")!!.readText())

val HealthcareServiceTypeVS = loadSimpleValueSet("HealthcareServiceTypeVS")
val PractitionerQualificationVS = loadSimpleValueSet("PractitionerQualificationVS")
