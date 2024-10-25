package de.gematik.ti.directory.fhir

import org.hl7.fhir.r4.model.*

fun Bundle.filterByType(resourceType: ResourceType): List<Resource> =
    entry
        .filter {
            it.resource.resourceType == resourceType
        }.map { it.resource }

fun Bundle.filterPractitionerRoles(): List<FHIRDirectoryEntry> =
    filterByType(ResourceType.PractitionerRole).map { resource ->
        val practitionerRole = resource as PractitionerRole
        val practitioner = findResource(practitionerRole.practitioner)?.resource?.let { it as Practitioner }

        FHIRDirectoryEntry(
            telematikID = practitioner?.identifier?.firstOrNull { it.system == "https://gematik.de/fhir/sid/telematik-id" }?.value,
            displayName = practitioner?.name?.firstOrNull()?.text,
            resourceType = ResourceType.PractitionerRole,
            practitionerRole = practitionerRole,
            practitioner = practitioner,
            location =
                practitionerRole.location
                    ?.mapNotNull {
                        findResource(it)?.resource
                    }?.map {
                        it as Location
                    }?.ifEmpty { null },
            endpoint =
                practitionerRole.endpoint
                    ?.mapNotNull {
                        findResource(it)?.resource
                    }?.map {
                        it as Endpoint
                    }?.ifEmpty { null },
        )
    }

fun Bundle.filterHealthcareServices(): List<FHIRDirectoryEntry> =
    filterByType(ResourceType.HealthcareService).map { resource ->
        val healthcareService = resource as HealthcareService
        val organization = findResource(healthcareService.providedBy)?.resource?.let { it as Organization }
        FHIRDirectoryEntry(
            telematikID = organization?.identifier?.firstOrNull { it.system == "https://gematik.de/fhir/sid/telematik-id" }?.value,
            displayName = organization?.name,
            resourceType = ResourceType.HealthcareService,
            healthcareService = healthcareService,
            organization = organization,
            location =
                healthcareService.location
                    ?.mapNotNull {
                        findResource(it)?.resource
                    }?.map {
                        it as Location
                    }?.ifEmpty { null },
            endpoint =
                healthcareService.endpoint
                    ?.mapNotNull {
                        findResource(it)?.resource
                    }?.map {
                        it as Endpoint
                    }?.ifEmpty { null },
        )
    }

fun Bundle.toDirectoryEntries(): List<FHIRDirectoryEntry> = filterPractitionerRoles() + filterHealthcareServices()

fun Bundle.findResource(reference: Reference): Bundle.BundleEntryComponent? {
    val id = IdType(reference.reference)
    return this.entry.find {
        it.resource?.fhirType() == id.resourceType && it.resource?.idElement?.idPart == id.idPart
    }
}
