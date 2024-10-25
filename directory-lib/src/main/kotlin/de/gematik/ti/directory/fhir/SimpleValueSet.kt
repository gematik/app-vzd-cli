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

private fun loadSimpleValueSet(name: String): SimpleValueSet =
    json.decodeFromString(SimpleValueSet::class.java.getResource("/de.gematik.fhir.directory/ValueSet-$name.json")!!.readText())

val HealthcareServiceTypeVS = loadSimpleValueSet("HealthcareServiceTypeVS")
val PractitionerQualificationVS = loadSimpleValueSet("PractitionerQualificationVS")
