package de.gematik.ti.directory.cli.fhir

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import org.hl7.fhir.r4.model.*

typealias ElaborateBundle = List<Any>

fun Bundle.findResource(reference: Reference): Bundle.BundleEntryComponent? {
    val id = IdType(reference.reference)
    return this.entry.find {
        it.resource?.fhirType() == id.resourceType && it.resource?.idElement?.idPart == id.idPart
    }
}

@Serializable
data class ElaboratePractitionerRole(
    val resourceType: String = "PractitionerRole",
    val id: String,
    var identifier: List<@Contextual Identifier>? = null,
    var practitioner: ElaboratePractitioner? = null,
    var location: List<@Contextual Location>? = null,
) {
    fun apply(practitionerRole: PractitionerRole, bundle: Bundle? = null): ElaboratePractitionerRole{
        this.identifier = practitionerRole.identifier
        this.practitioner = bundle?.findResource(practitionerRole.practitioner)?.resource?.let {
            ElaboratePractitioner(
                id = it.idElement.idPart
            ).apply(it as Practitioner, bundle)
        }
        this.location = practitionerRole.location.mapNotNull {
            bundle?.findResource(it)?.resource as Location
        }
        return this
    }
}

@Serializable
data class ElaboratePractitioner (
    val resourceType: String = "Practitioner",
    val id: String,
    var identifier: List<@Contextual Identifier> = emptyList(),
    var qualification: List<@Contextual Coding> = emptyList(),
    var name: List<@Contextual HumanName>? = null,
) {
    fun apply(practitioner: Practitioner, bundle: Bundle? = null): ElaboratePractitioner {
        this.identifier = practitioner.identifier
        this.qualification = practitioner.qualification.flatMap {
            it.code.coding
        }
        this.name = practitioner.name
        return this
    }

}
