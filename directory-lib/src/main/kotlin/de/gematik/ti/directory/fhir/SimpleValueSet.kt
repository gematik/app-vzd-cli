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
