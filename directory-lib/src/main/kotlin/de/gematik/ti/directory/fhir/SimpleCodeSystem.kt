package de.gematik.ti.directory.fhir

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

@Serializable
class SimpleConcept(
    val code: String,
    val display: String,
)

@Serializable
class SimpleCodeSystem(
    val name: String,
    val concept: List<SimpleConcept>,
) {
    fun displayFor(code: String): String {
        return concept.firstOrNull { it.code == code }?.display ?: code
    }
}

private val json = Json { ignoreUnknownKeys = true }

private fun loadSimpleCodeSystem(name: String): SimpleCodeSystem {
    return json.decodeFromString(SimpleCodeSystem::class.java.getResource("/de.gematik.fhir.directory/CodeSystem-$name.json")!!.readText())
}

val OrganizationProfessionOID = loadSimpleCodeSystem("OrganizationProfessionOID")