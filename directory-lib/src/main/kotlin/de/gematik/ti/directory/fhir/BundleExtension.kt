/*
Copyright 2026, gematik GmbH

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

*******

For additional notes and disclaimer from gematik and in case of changes
by gematik, find details in the "Readme" file.
*/

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
