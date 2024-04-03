package de.gematik.ti.directory.cli.fhir

import de.gematik.ti.directory.fhir.findResource
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import org.hl7.fhir.r4.model.*

typealias ElaborateBundle = List<Any>

fun Bundle.elaborateBundle(): ElaborateBundle {
    val entries =
        this.entry.mapNotNull {
            when (it.resource.resourceType) {
                ResourceType.PractitionerRole -> {
                    val role = it.resource as PractitionerRole
                    ElaboratePractitionerRole(id = role.idElement.idPart).apply(role, this)
                }

                ResourceType.HealthcareService -> {
                    val service = it.resource as HealthcareService
                    ElaborateHealthcareService(id = service.idElement.idPart).apply(service, this)
                }

                else -> {
                    null
                }
            }
        }
    return entries
}

@Serializable
data class ElaboratePractitionerRole(
    val resourceType: String = "PractitionerRole",
    val id: String,
    var identifier: List<@Contextual Identifier>? = null,
    var practitioner: ElaboratePractitioner? = null,
    var location: List<@Contextual Location>? = null,
    var endpoint: List<@Contextual Endpoint>? = null,
) {
    fun apply(
        practitionerRole: PractitionerRole,
        bundle: Bundle? = null
    ): ElaboratePractitionerRole {
        this.identifier = practitionerRole.identifier
        this.practitioner =
            bundle?.findResource(practitionerRole.practitioner)?.resource?.let {
                ElaboratePractitioner(
                    id = it.idElement.idPart,
                ).apply(it as Practitioner, bundle)
            }
        this.location =
            practitionerRole.location.mapNotNull {
                bundle?.findResource(it)?.resource
            }.map { it as Location }.ifEmpty {
                null
            }

        this.endpoint =
            practitionerRole.endpoint.mapNotNull {
                bundle?.findResource(it)?.resource
            }.map { it as Endpoint }.ifEmpty {
                null
            }

        return this
    }
}

@Serializable
data class ElaboratePractitioner(
    val resourceType: String = "Practitioner",
    val id: String,
    var identifier: List<@Contextual Identifier> = emptyList(),
    var qualification: List<@Contextual Coding> = emptyList(),
    var name: List<@Contextual HumanName>? = null,
) {
    fun apply(
        practitioner: Practitioner,
        bundle: Bundle? = null
    ): ElaboratePractitioner {
        this.identifier = practitioner.identifier
        this.qualification =
            practitioner.qualification.flatMap {
                it.code.coding
            }
        this.name = practitioner.name
        return this
    }
}

@Serializable
data class ElaborateHealthcareService(
    val resourceType: String = "HealthcareService",
    val id: String,
    var identifier: List<@Contextual Identifier>? = null,
    var organization: ElaborateOrganization? = null,
    var location: List<@Contextual Location>? = null,
    var endpoint: List<@Contextual Endpoint>? = null,
) {
    fun apply(
        healthcareService: HealthcareService,
        bundle: Bundle? = null
    ): ElaborateHealthcareService {
        this.identifier = healthcareService.identifier
        this.organization =
            bundle?.findResource(healthcareService.providedBy)?.resource?.let {
                ElaborateOrganization(
                    id = it.idElement.idPart,
                ).apply(it as Organization, bundle)
            }
        this.location =
            healthcareService.location.mapNotNull {
                bundle?.findResource(it)?.resource
            }.map { it as Location }.ifEmpty {
                null
            }

        this.endpoint =
            healthcareService.endpoint.mapNotNull {
                bundle?.findResource(it)?.resource
            }.map { it as Endpoint }.ifEmpty {
                null
            }

        return this
    }
}

@Serializable
data class ElaborateOrganization(
    val resourceType: String = "Organization",
    val id: String,
    var identifier: List<@Contextual Identifier>? = null,
    var name: String? = null,
    var type: List<@Contextual Coding>? = null,
) {
    fun apply(
        organization: Organization,
        bundle: Bundle? = null
    ): ElaborateOrganization {
        this.identifier = organization.identifier
        this.name = organization.name

        this.type = organization.type?.map { it.coding }?.flatten()?.ifEmpty { null }

        return this
    }
}
