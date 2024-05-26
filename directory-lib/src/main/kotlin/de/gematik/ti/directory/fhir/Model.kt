package de.gematik.ti.directory.fhir

import org.hl7.fhir.r4.model.*

data class HealthcareServiceEntry(
    val healthcareService: HealthcareService,
    val organization: Organization? = null,
    val location: List<Location>? = null,
    val endpoint: List<Endpoint>? = null,
)

data class PractitionerRoleEntry(
    val practitionerRole: PractitionerRole,
    val practitioner: Practitioner? = null,
    val location: List<Location>? = null,
    val endpoint: List<Endpoint>? = null,
)

/**
 * Combined entry for all resource types
 */
data class FHIRDirectoryEntry(
    val resourceType: ResourceType,
    val practitionerRole: PractitionerRole? = null,
    val practitioner: Practitioner? = null,
    val healthcareService: HealthcareService? = null,
    val organization: Organization? = null,
    val location: List<Location>? = null,
    val endpoint: List<Endpoint>? = null,
) {
    val telematikID: String?
        get() {
            if (practitioner != null) {
                return practitioner.identifier?.firstOrNull { it.system == "https://gematik.de/fhir/sid/telematik-id" }?.value
            } else if (organization != null) {
                return organization.identifier?.firstOrNull { it.system == "https://gematik.de/fhir/sid/telematik-id" }?.value
            } else {
                return "N/A"
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