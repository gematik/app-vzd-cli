package de.gematik.ti.directory.cli.admin

import de.gematik.ti.directory.admin.*
import de.gematik.ti.directory.util.AdmissionStatementInfo
import de.gematik.ti.directory.util.ExtendedCertificateDataDERSerializer
import de.gematik.ti.directory.util.NameInfo
import de.gematik.ti.directory.util.OCSPResponse
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import net.mamoe.yamlkt.Yaml

private var HumanDirectoryEntryYaml = Yaml {
    encodeDefaultValues = false
    serializersModule = SerializersModule {
        contextual(ExtendedCertificateDataDERSerializer)
    }
}

@Serializable
private class CertificateShortInfo(
    val subjectInfo: NameInfo,
    val admissionStatement: AdmissionStatementInfo,
    val issuer: String,
    val publicKeyAlgorithm: String,
    val serialNumber: String,
    val notBefore: LocalDateTime,
    val notAfter: LocalDateTime,
    val active: Boolean,
    var ocspResponse: OCSPResponse? = null,
)

@Serializable
private class KIMInfo(
    val fad: String,
    val mail: String,
    val version: String,
)

@Serializable
private class HumanDirectoryEntry(
    var uid: String?,

    var telematikID: String,
    var domainID: List<String>? = null,

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
    @Serializable(with = TemporaryEntryTypeSerializer::class)
    var entryType: Int? = null,

    // System
    var holder: List<String>? = null,
    var dataFromAuthority: Boolean? = null,
    var personalEntry: Boolean? = null,
    @Serializable(with = ForgivingLocalDateTimeSerializer::class)
    var changeDateTime: LocalDateTime? = null,

    // Internal
    var maxKOMLEadr: Int? = null,

    var meta: List<String>? = null,

    var userCertificate: List<CertificateShortInfo>? = null,
    var smartcards: List<Smartcard>? = null,

    var kim: List<KIMInfo>? = null,

    //
    var kind: DirectoryEntryKind,
)

object DirectoryEntryHumanSerializer : KSerializer<DirectoryEntry> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("CertificateDataDER", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: DirectoryEntry) {
        val surrogate = HumanDirectoryEntry(
            uid = value.directoryEntryBase.dn?.uid,
            telematikID = value.directoryEntryBase.telematikID,
            domainID = value.directoryEntryBase.domainID,

            // Names
            displayName = value.directoryEntryBase.displayName,
            cn = value.directoryEntryBase.cn,
            otherName = value.directoryEntryBase.otherName,
            organization = value.directoryEntryBase.organization,
            givenName = value.directoryEntryBase.givenName,
            sn = value.directoryEntryBase.sn,
            title = value.directoryEntryBase.title,

            // Addresses
            streetAddress = value.directoryEntryBase.streetAddress,
            postalCode = value.directoryEntryBase.postalCode,
            localityName = value.directoryEntryBase.localityName,
            stateOrProvinceName = value.directoryEntryBase.stateOrProvinceName,
            countryCode = value.directoryEntryBase.countryCode,

            // Professional
            professionOID = value.directoryEntryBase.professionOID,
            specialization = value.directoryEntryBase.specialization,
            entryType = value.directoryEntryBase.entryType,

            // System
            holder = value.directoryEntryBase.holder,
            dataFromAuthority = value.directoryEntryBase.dataFromAuthority,
            personalEntry = value.directoryEntryBase.personalEntry,
            changeDateTime = value.directoryEntryBase.changeDateTime,

            // Internal
            maxKOMLEadr = value.directoryEntryBase.maxKOMLEadr,

            meta = value.directoryEntryBase.meta,

            userCertificate = value.userCertificates?.mapNotNull {
                it.userCertificate?.certificateInfo?.let { certInfo ->
                    CertificateShortInfo(
                        subjectInfo = certInfo.subjectInfo,
                        admissionStatement = certInfo.admissionStatement,
                        issuer = certInfo.issuer,
                        publicKeyAlgorithm = certInfo.publicKeyAlgorithm,
                        serialNumber = certInfo.serialNumber,
                        notBefore = certInfo.notBefore,
                        notAfter = certInfo.notAfter,
                        active = it.active ?: true,
                        ocspResponse = certInfo.ocspResponse,
                    )
                }
            },

            smartcards = value.smartcards,

            kim = value.fachdaten?.let { it.mapNotNull { it.fad1 }.map { it.map { fad1 -> fad1.komLeData?.map { KIMInfo(fad1.dn.ou?.first() ?: "", it.mail, it.version) } ?: emptyList() } } }?.flatten()?.flatten(),

            kind = value.kind,

        )

        encoder.encodeSerializableValue(HumanDirectoryEntry.serializer(), surrogate)
    }

    override fun deserialize(decoder: Decoder): DirectoryEntry {
        throw NotImplementedError()
    }
}

fun DirectoryEntry.toHuman() = HumanDirectoryEntryYaml.encodeToString(DirectoryEntryHumanSerializer, this)
fun List<DirectoryEntry>.toHuman() = HumanDirectoryEntryYaml.encodeToString(ListSerializer(DirectoryEntryHumanSerializer), this)
