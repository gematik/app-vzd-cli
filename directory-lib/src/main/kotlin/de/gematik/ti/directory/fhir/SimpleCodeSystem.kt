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
val Holder = loadSimpleCodeSystem("HolderCS")
