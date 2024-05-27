package de.gematik.ti.directory.fhir

import org.hl7.fhir.r4.model.*

fun Bundle.filterByType(resourceType: ResourceType): List<Resource> {
    return entry.filter { it.resource.resourceType == resourceType }.map { it.resource }
}

fun Bundle.filterPractitionerRoles(): List<FHIRDirectoryEntry> {
    return filterByType(ResourceType.PractitionerRole).map { resource ->
        val practitionerRole = resource as PractitionerRole
        FHIRDirectoryEntry(
            resourceType = ResourceType.PractitionerRole,
            practitionerRole = practitionerRole,
            practitioner = findResource(practitionerRole.practitioner)?.resource?.let { it as Practitioner },
            location =
                practitionerRole.location?.mapNotNull {
                    findResource(it)?.resource
                }?.map {
                    it as Location
                }?.ifEmpty { null },
            endpoint =
                practitionerRole.endpoint?.mapNotNull {
                    findResource(it)?.resource
                }?.map {
                    it as Endpoint
                }?.ifEmpty { null },
        )
    }
}

fun Bundle.filterHealthcareServices(): List<FHIRDirectoryEntry> {
    return filterByType(ResourceType.HealthcareService).map { resource ->
        val healthcareService = resource as HealthcareService
        FHIRDirectoryEntry(
            resourceType = ResourceType.HealthcareService,
            healthcareService = healthcareService,
            organization = findResource(healthcareService.providedBy)?.resource?.let { it as Organization },
            location =
                healthcareService.location?.mapNotNull {
                    findResource(it)?.resource
                }?.map {
                    it as Location
                }?.ifEmpty { null },
            endpoint =
                healthcareService.endpoint?.mapNotNull {
                    findResource(it)?.resource
                }?.map {
                    it as Endpoint
                }?.ifEmpty { null },
        )
    }
}

fun Bundle.toDirectoryEntries(): List<FHIRDirectoryEntry> {
    return filterPractitionerRoles().map {
        FHIRDirectoryEntry(
            resourceType = ResourceType.PractitionerRole,
            practitionerRole = it.practitionerRole,
            practitioner = it.practitioner,
            location = it.location,
            endpoint = it.endpoint,
        )
    } +
        filterHealthcareServices().map {
            FHIRDirectoryEntry(
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
