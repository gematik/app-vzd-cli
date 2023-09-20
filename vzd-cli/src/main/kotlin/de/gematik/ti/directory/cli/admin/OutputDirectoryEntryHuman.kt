package de.gematik.ti.directory.cli.admin

import de.gematik.ti.directory.admin.*
import de.gematik.ti.directory.elaborate.*
import de.gematik.ti.directory.fhir.Coding
import de.gematik.ti.directory.pki.AdmissionStatementInfo
import de.gematik.ti.directory.pki.ExtendedCertificateDataDERSerializer
import de.gematik.ti.directory.pki.NameInfo
import de.gematik.ti.directory.pki.OCSPResponse
import kotlinx.datetime.Instant
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import net.mamoe.yamlkt.Yaml

private var humanDirectoryEntryYaml =
    Yaml {
        encodeDefaultValues = false
        serializersModule =
            SerializersModule {
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
    val notBefore: Instant,
    val notAfter: Instant,
    val active: Boolean,
    var ocspResponse: OCSPResponse? = null,
    var thumbprint: String? = null,
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
    var meta: List<String>? = null,
    var userCertificate: List<CertificateShortInfo>? = null,
    var smartcards: List<Smartcard>? = null,
    var kim: List<ElaborateKIMAddress>? = null,
    var kind: String? = null,
    var fhirResourceType: DirectoryEntryResourceType? = null,
    var active: Boolean? = null,
    var validationResult: ValidationResult? = null,
)

object DirectoryEntryHumanSerializer : KSerializer<DirectoryEntry> {
    override val descriptor: SerialDescriptor = HumanDirectoryEntry.serializer().descriptor

    override fun serialize(
        encoder: Encoder,
        value: DirectoryEntry,
    ) {
        val entry = value.elaborate()
        val surrogate =
            HumanDirectoryEntry(
                uid = entry.base.dn?.uid,
                telematikID = entry.base.telematikID,
                domainID = entry.base.domainID,
                // Names
                displayName = entry.base.displayName,
                cn = entry.base.cn,
                otherName = entry.base.otherName,
                organization = entry.base.organization,
                givenName = entry.base.givenName,
                sn = entry.base.sn,
                title = entry.base.title,
                // Addresses
                streetAddress = entry.base.streetAddress,
                postalCode = entry.base.postalCode,
                localityName = entry.base.localityName,
                stateOrProvinceName = entry.base.stateOrProvinceName,
                countryCode = entry.base.countryCode,
                // Professional
                professionOID = entry.base.professionOID,
                specialization = entry.base.specialization,
                entryType = entry.base.entryType,
                // System
                holder = entry.base.holder,
                dataFromAuthority = entry.base.dataFromAuthority,
                personalEntry = entry.base.personalEntry,
                changeDateTime = entry.base.changeDateTime,
                // Internal
                maxKOMLEadr = entry.base.maxKOMLEadr,
                meta = entry.base.meta,
                userCertificate =
                    entry.userCertificates?.mapNotNull {
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
                                thumbprint = certInfo.thumbprint,
                            )
                        }
                    },
                smartcards = entry.smartcards,
                kim = entry.kimAddresses,
                kind = entry.base.kind,
                fhirResourceType = entry.base.fhirResourceType,
                active = entry.base.active,
                validationResult = entry.validationResult,
            )

        encoder.encodeSerializableValue(HumanDirectoryEntry.serializer(), surrogate)
    }

    override fun deserialize(decoder: Decoder): DirectoryEntry {
        throw NotImplementedError()
    }
}

fun DirectoryEntry.toHuman() = humanDirectoryEntryYaml.encodeToString(DirectoryEntryHumanSerializer, this)

fun List<DirectoryEntry>.toHuman() = humanDirectoryEntryYaml.encodeToString(ListSerializer(DirectoryEntryHumanSerializer), this)
