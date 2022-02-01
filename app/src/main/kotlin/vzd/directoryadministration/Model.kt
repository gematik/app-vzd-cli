package vzd.directoryadministration

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DistinguishedName(
    var uid: String,
    var dc:  List<String>?,
    var ou: List<String>?,
    var cn: String?,
)

@Serializable
data class BaseDirectoryEntry(
    var cn: String,
    var dn: DistinguishedName,
    var givenName: String? = null,
    var sn: String? = null,
    var displayName: String? = null,
    var streetAddress: String? = null,
    var postalCode: String? = null,
    var countryCode: String? = null,
    var localityName: String? = null,
    var stateOrProvinceName: String? = null,
    var title: String? = null,
    var organization: String? = null,
    var otherName: String? = null,
    var telematikID: String? = null,
    var specialization: List<String>? = null,
    var domainID: List<String>? = null,
    var holder: List<String>? = null,
    var items: String? = null,
    var maxKOMLEadr: Int? = null,
    var personalEntry: Boolean? = null,
    var dataFromAuthority: Boolean? = null,
    var changeDateTime: String? = null
)

@Serializable
data class UserCertificate(
    var dn: DistinguishedName,
    var entryType: String? = null,
    var telematikID: String? = null,
    var professionOID: List<String>? = null,
    var usage: List<String>? = null,
    var userCertificate: String? = null,
    var description: String? = null
)

@Serializable
data class FAD1(
    var dn: DistinguishedName,
    var mail: List<String>? = null,
    @SerialName("KOM-LE_Version")
    var komleVersion: String? = null,
)

@Serializable
data class Fachdaten (
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
    var fachdaten: List<Fachdaten>? = null
)