package de.gematik.ti.directory.cli.fhir

import de.gematik.ti.directory.fhir.FHIRDirectoryEntry
import kotlinx.serialization.*
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import org.hl7.fhir.r4.model.Address
import org.hl7.fhir.r4.model.Coding
import org.hl7.fhir.r4.model.HealthcareService
import org.hl7.fhir.r4.model.HumanName
import org.hl7.fhir.r4.model.Identifier
import org.hl7.fhir.r4.model.Practitioner
import org.hl7.fhir.r4.model.PractitionerRole
import org.hl7.fhir.r4.model.Reference

val FHIRSerializerModule =
    SerializersModule {
        contextual(FHIRDirectoryEntrySerializer)
        contextual(HealthcareServiceSerializer)
        contextual(PractitionerRoleSerializer)
        contextual(CodingSerializer)
        contextual(IdentifierSerializer)
        contextual(HumanNameSerializer)
        contextual(AddressSerializer)
        contextual(LocationSerializer)
        contextual(EndpointSerializer)
        contextual(ReferenceSerializer)
        contextual(OrganizationSerializer)
        contextual(PractitionerSerializer)
    }

/**
 * Serializer für FHIR Directory Entry
 */
object FHIRDirectoryEntrySerializer : KSerializer<FHIRDirectoryEntry> {
    @Serializable
    @SerialName("FHIRDirectoryEntry")
    data class FHIRDirectoryEntrySurrogate(
        val resourceType: String,
        val telematikID: String? = null,
        val displayName: String? = null,
        val practitionerRole: @Contextual PractitionerRole? = null,
        val practitioner: @Contextual Practitioner? = null,
        val healthcareService: @Contextual HealthcareService? = null,
        val organization: @Contextual org.hl7.fhir.r4.model.Organization? = null,
        val location: List<@Contextual org.hl7.fhir.r4.model.Location>? = null,
        val endpoint: List<@Contextual org.hl7.fhir.r4.model.Endpoint>? = null,
    )

    override val descriptor: SerialDescriptor = FHIRDirectoryEntrySurrogate.serializer().descriptor

    override fun serialize(
        encoder: Encoder,
        value: FHIRDirectoryEntry,
    ) {
        val surrogate =
            FHIRDirectoryEntrySurrogate(
                resourceType = value.resourceType.name,
                telematikID = value.telematikID,
                displayName = value.displayName,
                practitionerRole = value.practitionerRole,
                practitioner = value.practitioner,
                healthcareService = value.healthcareService,
                organization = value.organization,
                location = value.location,
                endpoint = value.endpoint,
            )
        encoder.encodeSerializableValue(FHIRDirectoryEntrySurrogate.serializer(), surrogate)
    }

    override fun deserialize(decoder: Decoder): FHIRDirectoryEntry {
        throw NotImplementedError()
    }
}

/**
 * Elaborate Serializer für FHIR HealthcareService
 */
object HealthcareServiceSerializer : KSerializer<HealthcareService> {
    @Serializable
    @SerialName("HealthcareService")
    data class HealthcareServiceSurrogate(
        val id: String,
        val identifier: List<@Contextual Identifier>? = null,
        val providedBy: @Contextual Reference? = null,
        val location: List<@Contextual String>? = null,
        val endpoint: List<@Contextual String>? = null,
    )

    override val descriptor: SerialDescriptor = HealthcareServiceSurrogate.serializer().descriptor

    override fun serialize(
        encoder: Encoder,
        value: HealthcareService,
    ) {
        val surrogate =
            HealthcareServiceSurrogate(
                id = value.idElement.idPart,
                identifier = value.identifier.ifEmpty { null },
                providedBy = value.providedBy,
                location = value.location.map { it.reference }.ifEmpty { null },
                endpoint = value.endpoint.map { it.reference }.ifEmpty { null },
            )
        encoder.encodeSerializableValue(HealthcareServiceSurrogate.serializer(), surrogate)
    }

    override fun deserialize(decoder: Decoder): HealthcareService {
        throw NotImplementedError()
    }
}

/**
 * Serializer für FHIR PractitionerRole
 */
object PractitionerRoleSerializer : KSerializer<PractitionerRole> {
    @Serializable
    @SerialName("PractitionerRole")
    data class PractitionerRoleSurrogate(
        val id: String,
        val identifier: List<@Contextual Identifier>? = null,
        val practitioner: @Contextual Reference? = null,
        val location: List<@Contextual String>? = null,
        val endpoint: List<@Contextual String>? = null,
    )

    override val descriptor: SerialDescriptor = PractitionerRoleSurrogate.serializer().descriptor

    override fun serialize(
        encoder: Encoder,
        value: PractitionerRole,
    ) {
        val surrogate =
            PractitionerRoleSurrogate(
                id = value.idElement.idPart,
                identifier = value.identifier.ifEmpty { null },
                practitioner = value.practitioner,
                location = value.location.map { it.reference }.ifEmpty { null },
                endpoint = value.endpoint.map { it.reference }.ifEmpty { null },
            )
        encoder.encodeSerializableValue(PractitionerRoleSurrogate.serializer(), surrogate)
    }

    override fun deserialize(decoder: Decoder): PractitionerRole {
        throw NotImplementedError()
    }
}

/**
 * Serializer für FHIR Coding
 */
object CodingSerializer : KSerializer<Coding> {
    @Serializable
    @SerialName("Coding")
    data class CodingSurrogate(
        val system: String,
        val code: String,
        val display: String? = null,
    )

    override val descriptor: SerialDescriptor = CodingSurrogate.serializer().descriptor

    override fun serialize(
        encoder: Encoder,
        value: Coding,
    ) {
        val surrogate =
            CodingSurrogate(
                system = value.system,
                code = value.code,
                display = value.display,
            )
        encoder.encodeSerializableValue(CodingSurrogate.serializer(), surrogate)
    }

    override fun deserialize(decoder: Decoder): Coding {
        throw NotImplementedError()
    }
}

/**
 * Serializer für FHIR Identifier
 */
object IdentifierSerializer : KSerializer<Identifier> {
    @Serializable
    @SerialName("Identifier")
    data class IdentifierSurrogate(
        val system: String,
        val value: String,
    )

    override val descriptor: SerialDescriptor = IdentifierSurrogate.serializer().descriptor

    override fun serialize(
        encoder: Encoder,
        value: Identifier,
    ) {
        val surrogate =
            IdentifierSurrogate(
                system = value.system,
                value = value.value,
            )
        encoder.encodeSerializableValue(IdentifierSurrogate.serializer(), surrogate)
    }

    override fun deserialize(decoder: Decoder): Identifier {
        throw NotImplementedError()
    }
}

/**
 * Serializer for HumanName
 */
object HumanNameSerializer : KSerializer<HumanName> {
    @Serializable
    @SerialName("HumanName")
    data class HumanNameSurrogate(
        val family: String? = null,
        val given: List<String>? = null,
        val prefix: List<String>? = null,
        val suffix: List<String>? = null,
        val use: String? = null,
        val text: String? = null,
    )

    override val descriptor: SerialDescriptor = HumanNameSurrogate.serializer().descriptor

    override fun serialize(
        encoder: Encoder,
        value: HumanName,
    ) {
        val surrogate =
            HumanNameSurrogate(
                family = value.family,
                given = value.given.map { it.value }.ifEmpty { null },
                prefix = value.prefix.map { it.value }.ifEmpty { null },
                suffix = value.suffix.map { it.value }.ifEmpty { null },
                use = value.use?.name?.lowercase(),
                text = value.text,
            )
        encoder.encodeSerializableValue(HumanNameSurrogate.serializer(), surrogate)
    }

    override fun deserialize(decoder: Decoder): HumanName {
        throw NotImplementedError()
    }
}

/**
 * Serializer for FHIR Address
 */
object AddressSerializer : KSerializer<Address> {
    @Serializable
    @SerialName("Address")
    data class AddressSurrogate(
        val use: String? = null,
        val type: String? = null,
        val text: String? = null,
        val line: List<String>? = null,
        val city: String? = null,
        val district: String? = null,
        val state: String? = null,
        val postalCode: String? = null,
        val country: String? = null,
        val period: String? = null,
    )

    override val descriptor: SerialDescriptor = AddressSurrogate.serializer().descriptor

    override fun serialize(
        encoder: Encoder,
        value: Address,
    ) {
        val surrogate =
            AddressSurrogate(
                use = value.use?.name?.lowercase(),
                type = value.type?.name?.lowercase(),
                text = value.text,
                line = value.line.map { it.value },
                city = value.city,
                district = value.district,
                state = value.state,
                postalCode = value.postalCode,
                country = value.country,
                period = value.period?.start?.toString(),
            )
        encoder.encodeSerializableValue(AddressSurrogate.serializer(), surrogate)
    }

    override fun deserialize(decoder: Decoder): Address {
        throw NotImplementedError()
    }
}

/**
 * Serializer for FHIR Location
 */
object LocationSerializer : KSerializer<org.hl7.fhir.r4.model.Location> {
    @Serializable
    @SerialName("Location")
    data class LocationSurrogate(
        val id: String,
        val identifier: List<@Contextual Identifier>? = null,
        val name: String? = null,
        val address: @Contextual Address? = null,
    )

    override val descriptor: SerialDescriptor = LocationSurrogate.serializer().descriptor

    override fun serialize(
        encoder: Encoder,
        value: org.hl7.fhir.r4.model.Location,
    ) {
        val surrogate =
            LocationSurrogate(
                id = value.idElement.idPart,
                identifier = value.identifier.ifEmpty { null },
                name = value.name,
                address = value.address,
            )
        encoder.encodeSerializableValue(LocationSurrogate.serializer(), surrogate)
    }

    override fun deserialize(decoder: Decoder): org.hl7.fhir.r4.model.Location {
        throw NotImplementedError()
    }
}

/**
 * Serializer for FHIR Endpoint
 */
object EndpointSerializer : KSerializer<org.hl7.fhir.r4.model.Endpoint> {
    @Serializable
    @SerialName("Endpoint")
    data class EndpointSurrogate(
        val id: String,
        val identifier: List<@Contextual Identifier>? = null,
        val connectionType: @Contextual Coding? = null,
        val name: String? = null,
        val status: String? = null,
        val payloadType: List<@Contextual Coding>? = null,
        val payloadMimeType: List<String>? = null,
        val address: String? = null,
        val header: List<String>? = null,
    )

    override val descriptor: SerialDescriptor = EndpointSurrogate.serializer().descriptor

    override fun serialize(
        encoder: Encoder,
        value: org.hl7.fhir.r4.model.Endpoint,
    ) {
        val surrogate =
            EndpointSurrogate(
                id = value.idElement.idPart,
                identifier = value.identifier.ifEmpty { null },
                connectionType = value.connectionType,
                name = value.name,
                status = value.status?.name?.lowercase(),
                payloadType = value.payloadType.flatMap { it.coding }.ifEmpty { null },
                payloadMimeType = value.payloadMimeType.map { it.value }.ifEmpty { null },
                address = value.address,
                header = value.header.map { it.value }.ifEmpty { null },
            )
        encoder.encodeSerializableValue(EndpointSurrogate.serializer(), surrogate)
    }

    override fun deserialize(decoder: Decoder): org.hl7.fhir.r4.model.Endpoint {
        throw NotImplementedError()
    }
}

/**
 * Serializer for FHIR Reference
 */
object ReferenceSerializer : KSerializer<Reference> {
    @Serializable
    @SerialName("Reference")
    data class ReferenceSurrogate(
        val reference: String,
    )

    override val descriptor: SerialDescriptor = ReferenceSurrogate.serializer().descriptor

    override fun serialize(
        encoder: Encoder,
        value: Reference,
    ) {
        val surrogate =
            ReferenceSurrogate(
                reference = value.reference,
            )
        encoder.encodeSerializableValue(ReferenceSurrogate.serializer(), surrogate)
    }

    override fun deserialize(decoder: Decoder): Reference {
        throw NotImplementedError()
    }
}

/**
 * Serializer for FHIR Organization
 */
object OrganizationSerializer : KSerializer<org.hl7.fhir.r4.model.Organization> {
    @Serializable
    @SerialName("Organization")
    data class OrganizationSurrogate(
        val id: String,
        val identifier: List<@Contextual Identifier>? = null,
        val name: String? = null,
        val address: List<@Contextual Address>? = null,
    )

    override val descriptor: SerialDescriptor = OrganizationSurrogate.serializer().descriptor

    override fun serialize(
        encoder: Encoder,
        value: org.hl7.fhir.r4.model.Organization,
    ) {
        val surrogate =
            OrganizationSurrogate(
                id = value.idElement.idPart,
                identifier = value.identifier.ifEmpty { null },
                name = value.name,
                address = value.address,
            )
        encoder.encodeSerializableValue(OrganizationSurrogate.serializer(), surrogate)
    }

    override fun deserialize(decoder: Decoder): org.hl7.fhir.r4.model.Organization {
        throw NotImplementedError()
    }
}

/**
 * Serializer for FHIR Practitioner
 */
object PractitionerSerializer : KSerializer<Practitioner> {
    @Serializable
    @SerialName("Practitioner")
    data class PractitionerSurrogate(
        val id: String,
        val identifier: List<@Contextual Identifier>? = null,
        val name: List<@Contextual HumanName>? = null,
    )

    override val descriptor: SerialDescriptor = PractitionerSurrogate.serializer().descriptor

    override fun serialize(
        encoder: Encoder,
        value: org.hl7.fhir.r4.model.Practitioner,
    ) {
        val surrogate =
            PractitionerSurrogate(
                id = value.idElement.idPart,
                identifier = value.identifier.ifEmpty { null },
                name = value.name.ifEmpty { null },
            )
        encoder.encodeSerializableValue(PractitionerSurrogate.serializer(), surrogate)
    }

    override fun deserialize(decoder: Decoder): Practitioner {
        throw NotImplementedError()
    }
}
