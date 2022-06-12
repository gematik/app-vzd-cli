package vzd.admin.client

import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import vzd.pki.CertificateDataDER

@Serializable
@SerialName("Error")
data class AttributeError(
    val attributeName: String? = null,
    val attributeError: String? = null,
)

@Serializable
data class Contact(
    val name: String? = null,
    val url: String? = null,
    val email: String? = null,
)

@Serializable
data class License(
    val name: String?,
    val url: String?,
)

@Serializable
data class InfoObject(
    val title: String,
    val version: String,
    val description: String? = null,
    val termsOfService: String? = null,
    val contact: Contact? = null,
    val license: License? = null,
)

@Serializable
data class DistinguishedName(
    var uid: String,
    var dc: List<String>? = null,
    var ou: List<String>? = null,
    var cn: String? = null,
)

@Serializable
data class BaseDirectoryEntry(
    // Identifier
    var telematikID: String,
    var domainID: List<String>? = null,
    // TODO: YAML decode is not working when I add this
    // @Contextual
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
    var professionOID: List<String>? = null,
    var specialization: List<String>? = null,
    var entryType: List<String>? = null,

    // System
    var holder: List<String>? = null,
    var dataFromAuthority: Boolean? = null,
    var personalEntry: Boolean? = null,
    var changeDateTime: String? = null,

    // Internal
    var maxKOMLEadr: Int? = null,
)

@Serializable
data class UpdateBaseDirectoryEntry(
    var givenName: String? = null,
    var sn: String? = null,
    var telematikID: String? = null,
    var displayName: String? = null,
    var streetAddress: String? = null,
    var postalCode: String? = null,
    var countryCode: String? = null,
    var localityName: String? = null,
    var stateOrProvinceName: String? = null,
    var title: String? = null,
    var organization: String? = null,
    var otherName: String? = null,
    var specialization: List<String>? = null,
    var domainID: List<String>? = null,
    var holder: List<String>? = null,
    var maxKOMLEadr: Int? = null,
)

@Serializable
data class UserCertificate(
    @Contextual
    var dn: DistinguishedName? = null,
    var entryType: String? = null,
    var telematikID: String? = null,
    var professionOID: List<String>? = null,
    var usage: List<String>? = null,
    @Contextual
    var userCertificate: CertificateDataDER? = null,
    var description: String? = null,
)

@Serializable
data class FAD1(
    @Contextual
    var dn: DistinguishedName,
    var mail: List<String>? = null,
    @SerialName("KOM-LE_Version")
    var komleVersion: String? = null,
)

@Serializable
data class Fachdaten(
    @Contextual
    var dn: DistinguishedName,
    @SerialName("FAD1")
    var fad1: List<FAD1>? = null,
)

@Serializable
data class DirectoryEntry(
    @SerialName("DirectoryEntryBase")
    var directoryEntryBase: BaseDirectoryEntry,
    var userCertificates: List<UserCertificate>? = null,
    @SerialName("Fachdaten")
    var fachdaten: List<Fachdaten>? = null,
)

@Serializable
data class CreateDirectoryEntry(
    @SerialName("DirectoryEntryBase")
    var directoryEntryBase: BaseDirectoryEntry? = null,
    var userCertificates: List<UserCertificate>? = null,
)
