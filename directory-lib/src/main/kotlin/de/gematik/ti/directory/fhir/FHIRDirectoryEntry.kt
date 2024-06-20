package de.gematik.ti.directory.fhir

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import org.hl7.fhir.r4.model.*

/**
 * Combined entry for all resource types
 */
@Serializable
data class FHIRDirectoryEntry(
    val telematikID: String?,
    val displayName: String?,
    val resourceType: ResourceType,
    val organization: @Contextual Organization? = null,
    val healthcareService: @Contextual HealthcareService? = null,
    val practitioner: @Contextual Practitioner? = null,
    val practitionerRole: @Contextual PractitionerRole? = null,
    val location: List<@Contextual Location>? = null,
    val endpoint: List<@Contextual Endpoint>? = null,
)
