package vzd.admin.client

import kotlinx.serialization.*
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable
@SerialName("Error")
data class AttributeError (
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
    var telematikID: String,
    var cn: String? = null,
    //TODO: YAML decode is not working when I add this
    //@Contextual
    var dn: DistinguishedName? = null,
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
    var specialization: List<String>? = null,
    var domainID: List<String>? = null,
    var holder: List<String>? = null,
    var maxKOMLEadr: Int? = null,
    var personalEntry: Boolean? = null,
    var dataFromAuthority: Boolean? = null,
    var changeDateTime: String? = null
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

/**
 * Simple datatype for base64 encoded certificates to differentiate them from plain strings
 */
@Serializable(with= CertificateDataDERSerializer::class)
data class CertificateDataDER (
    var base64String: String
)

/**
 * Serializes {CertificateDataDER} to primitive string.
 */
object CertificateDataDERSerializer : KSerializer<CertificateDataDER> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("CertificateDataDER", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: CertificateDataDER) {
        encoder.encodeString(value.base64String)
    }

    override fun deserialize(decoder: Decoder): CertificateDataDER {
        return CertificateDataDER(decoder.decodeString())
    }
}

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
    var description: String? = null
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
data class Fachdaten (
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
    var fachdaten: List<Fachdaten>? = null
)

@Serializable
data class CreateDirectoryEntry(
    @SerialName("DirectoryEntryBase")
    var directoryEntryBase: BaseDirectoryEntry? = null,
    var userCertificates: List<UserCertificate>? = null,
)