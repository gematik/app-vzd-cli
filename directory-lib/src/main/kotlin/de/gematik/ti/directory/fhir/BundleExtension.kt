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
data class DirectoryEntry(
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

fun Bundle.filterByType(resourceType: ResourceType): List<Resource> {
    return entry.filter { it.resource.resourceType == resourceType }.map { it.resource }
}

fun Bundle.filterPractitionerRoles(): List<PractitionerRoleEntry> {
    return filterByType(ResourceType.PractitionerRole).map {
        val practitionerRole = it as PractitionerRole
        PractitionerRoleEntry(
            practitionerRole = practitionerRole,
            practitioner = findResource(practitionerRole.practitioner)?.resource as Practitioner?,
            location = practitionerRole.location.map { findResource(it)?.resource as Location }.ifEmpty { null },
            endpoint = practitionerRole.endpoint.map { findResource(it)?.resource as Endpoint }.ifEmpty { null },
        )
    }
}

fun Bundle.filterHealthcareServices(): List<HealthcareServiceEntry> {
    return filterByType(ResourceType.HealthcareService).map {
        val healthcareService = it as HealthcareService
        HealthcareServiceEntry(
            healthcareService = healthcareService,
            organization = findResource(healthcareService.providedBy)?.resource as Organization?,
            location = healthcareService.location.map { findResource(it)?.resource as Location }.ifEmpty { null },
            endpoint = healthcareService.endpoint.map { findResource(it)?.resource as Endpoint }.ifEmpty { null },
        )
    }
}

fun Bundle.toDirectoryEntries(): List<DirectoryEntry> {
    return filterPractitionerRoles().map {
        DirectoryEntry(
            resourceType = ResourceType.PractitionerRole,
            practitionerRole = it.practitionerRole,
            practitioner = it.practitioner,
            location = it.location,
            endpoint = it.endpoint,
        )
    } +
        filterHealthcareServices().map {
            DirectoryEntry(
                resourceType = ResourceType.HealthcareService,
                healthcareService = it.healthcareService,
                organization = it.organization,
                location = it.location,
                endpoint = it.endpoint,
            )
        }
}

public fun Bundle.findResource(reference: Reference): Bundle.BundleEntryComponent? {
    val id = IdType(reference.reference)
    return this.entry.find {
        it.resource?.fhirType() == id.resourceType && it.resource?.idElement?.idPart == id.idPart
    }
}
