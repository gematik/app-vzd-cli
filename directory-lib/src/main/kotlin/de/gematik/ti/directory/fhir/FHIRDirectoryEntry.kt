package de.gematik.ti.directory.fhir

import kotlinx.serialization.Contextual
import org.hl7.fhir.r4.model.*

/**
 * Combined entry for all resource types
 */
data class FHIRDirectoryEntry(
    val resourceType: ResourceType,
    val practitionerRole: @Contextual PractitionerRole? = null,
    val practitioner: @Contextual Practitioner? = null,
    val healthcareService: @Contextual HealthcareService? = null,
    val organization: @Contextual Organization? = null,
    val location: List<@Contextual Location>? = null,
    val endpoint: List<@Contextual Endpoint>? = null,
) {
    val telematikID: String?
        get() {
            if (practitioner != null) {
                return practitioner.identifier?.firstOrNull { it.system == "https://gematik.de/fhir/sid/telematik-id" }?.value
            } else if (organization != null) {
                return organization.identifier?.firstOrNull { it.system == "https://gematik.de/fhir/sid/telematik-id" }?.value
            } else {
                return null
            }
        }

    val displayName: String?
        get() {
            return when (resourceType) {
                ResourceType.PractitionerRole -> practitioner?.name?.firstOrNull()?.text
                ResourceType.HealthcareService -> organization?.name
                else -> null
            }
        }
}
