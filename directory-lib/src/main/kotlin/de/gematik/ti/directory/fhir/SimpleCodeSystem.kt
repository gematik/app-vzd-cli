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

private fun loadSimpleCodeSystem(name: String): SimpleCodeSystem =
    json.decodeFromString(SimpleCodeSystem::class.java.getResource("/de.gematik.fhir.directory/CodeSystem-$name.json")!!.readText())

val OrganizationProfessionOID = loadSimpleCodeSystem("OrganizationProfessionOID")
val PractitionerProfessionOID = loadSimpleCodeSystem("PractitionerProfessionOID")
val PharmacyTypeCS = loadSimpleCodeSystem("PharmacyTypeCS")
val Holder = loadSimpleCodeSystem("HolderCS")
