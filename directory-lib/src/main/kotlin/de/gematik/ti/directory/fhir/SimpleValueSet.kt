package de.gematik.ti.directory.fhir

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString

@Serializable
data class SimpleValueSet(
    val name: String,
    val compose: ComposeBackboneElement? = null,
) {
    fun displayFor(system: String, code: String): String? {
        return compose?.include?.firstOrNull { it.system == system }?.concept?.firstOrNull { it.code == code }?.display
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

private fun loadSimpleValueSet(name: String): SimpleValueSet {
    return json.decodeFromString(SimpleValueSet::class.java.getResource("/de.gematik.fhir.directory/ValueSet-$name.json")!!.readText())
}

val HealthcareServiceSpecialtyVS = loadSimpleValueSet("HealthcareServiceSpecialtyVS")
val PractitionerQualificationVS = loadSimpleValueSet("PractitionerQualificationVS")
