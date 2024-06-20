package de.gematik.ti.directory.admin

import de.gematik.ti.directory.elaborate.*
import de.gematik.ti.directory.pki.CertificateDataDER
import kotlinx.datetime.Instant
import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

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
    var entryType: List<String?>? = null,
    // System
    var holder: List<String>? = null,
    var dataFromAuthority: Boolean? = null,
    var personalEntry: Boolean? = null,
    var changeDateTime: Instant? = null,
    // Internal
    var maxKOMLEadr: Int? = null,
    // Misc
    var active: Boolean = true,
    var meta: List<String>? = null,
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
    var entryType: List<String?>? = null,
    var holder: List<String>? = null,
    var maxKOMLEadr: Int? = null,
    var active: Boolean? = true,
    var meta: List<String>? = null,
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
    var active: Boolean? = null,
    var serialNumber: String? = null,
    var issuer: String? = null,
    var publicKeyAlgorithm: String? = null,
    var notBefore: Instant? = null,
    var notAfter: Instant? = null,
)

@Serializable
data class KomLeData(
    val mail: String,
    val version: String,
)

@Serializable
data class FAD1(
    @Contextual
    var dn: DistinguishedName,
    var mail: List<String>? = null,
    @SerialName("KOM-LE_Version")
    var komleVersion: String? = null,
    var komLeData: List<KomLeData>? = null,
    var kimData: List<KIMData>? = null,
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

@Serializable
data class SearchControlValue(
    var size: Int,
    var cookie: String,
)

@Serializable
data class ReadDirectoryEntryForSyncResponse(
    var searchControlValue: SearchControlValue,
    var directoryEntries: List<DirectoryEntry>,
)

@Serializable
@Suppress("ktlint:standard:enum-entry-name-case")
enum class Operation {
    add_Directory_Entry,
    modify_Directory_Entry,
    delete_Directory_Entry,
    add_Directory_Entry_Certificate,
    delete_Directory_Entry_Certificate,

    @SerialName("add_Directory_FA-Attributes")
    add_Directory_FA_Attributes,

    @SerialName("modify_Directory_FA-Attributes")
    modify_Directory_FA_Attributes,

    @SerialName("delete_Directory_FA-Attributes")
    delete_Directory_FA_Attributes,

    // TODO: not in OpenAPI
    stateSwitch_Directory_Entry,
}

@Serializable
data class LogEntry(
    val clientID: String?,
    val logTime: Instant,
    val uid: String,
    val telematikID: String?,
    val operation: Operation,
    val noDataChanged: Boolean,
)

@Serializable
data class Error(
    val message: String,
    val errors: List<InnerError>?,
)

@Serializable
data class InnerError(
    val attributeName: String,
    val attributeError: String,
)

/**
 *  kimData:
 *           type: array
 *           items:
 *             type: object
 *             properties:
 *               mail:
 *                 type: string
 *                 description: 'E-Mail-Adresse'
 *               version:
 *                 type: string
 *                 example: 1.5+
 *                 description: 'Die höchste Version der KIM Clientmodule für diese KIM-Mail-Adresse'
 *               appTags:
 *                 type: array
 *                 items:
 *                   type: string
 *                   example:
 *                     - eEB;V1.0
 *                     - DALE-UV;Einsendung;V1.0
 *                   description: 'Anwendungskennzeichen, welche diese KIM-Mail-Adresse verarbeiten kann'
 */
@Serializable
data class KIMData(
    val mail: String,
    val version: String,
    val appTags: List<String>? = null,
)
