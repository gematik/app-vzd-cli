package de.gematik.ti.directory.elaborate

import de.gematik.ti.directory.admin.DistinguishedName
import de.gematik.ti.directory.admin.UserCertificate
import de.gematik.ti.directory.fhir.Coding
import de.gematik.ti.directory.validation.Finding
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class ElaborateDirectoryEntry(
    val kind: DirectoryEntryKind,
    val base: ElaborateBaseDirectoryEntry,
    val userCertificates: List<UserCertificate>? = null,
    val kimAddresses: List<ElaborateKIMAddress>? = null,
    val smartcards: List<Smartcard>? = null,
    var validationResult: ValidationResult? = null,
)

@Serializable
data class ValidationResult(
    val base: Map<String, List<Finding>>?,
)

@Serializable
data class ElaborateBaseDirectoryEntry(
    var kind: DirectoryEntryKind,
    var fhirResourceType: DirectoryEntryResourceType,

    // Identifier
    var telematikID: String,
    var domainID: List<String>? = null,
    var dn: DistinguishedName? = null,
    // Names
    var displayName: String? = null,
    var cn: String? = null,
    var otherName: String? = null,
    var organization: String? = null,
    var givenName: String? = null,
    var sn: String? = null,
    var title: String? = null,

    // Addresses
    var streetAddress: String? = null,
    var postalCode: String? = null,
    var localityName: String? = null,
    var stateOrProvinceName: String? = null,
    var countryCode: String? = null,

    // Professional
    var professionOID: List<Coding>? = null,
    var specialization: List<Coding>? = null,
    var entryType: List<String?>? = null,

    // System
    var holder: List<Coding>? = null,
    var dataFromAuthority: Boolean? = null,
    var personalEntry: Boolean? = null,
    var changeDateTime: Instant? = null,

    // Internal
    var maxKOMLEadr: Int? = null,

    // Misc
    var active: Boolean,
    var meta: List<String>? = null,
    )

enum class SmartcardType {
    HBA,
    HBA2_1,
    SMCB,
    SMCB2_1,
}

@Serializable
data class Smartcard(
    val type: SmartcardType,
    val notBefore: Instant,
    val notAfter: Instant,
    val active: Boolean,
    val certificateSerialNumbers: List<String>,
)

@Serializable
data class ElaborateKIMAddress(
    val mail: String,
    val version: String,
    val provider: ElaborateKIMProvider?,
)

@Serializable
data class ElaborateKIMProvider(
    val code: String,
    val display: String,
)
