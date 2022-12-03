package de.gematik.ti.directory.admin

import de.gematik.ti.directory.util.CertificateInfo
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable
data class SerializableValidationError(
    val dataPath: String,
    val message: String
)

@Serializable
data class SerializableValidationResult(
    val errors: List<SerializableValidationError>
)

enum class SmartcardType {
    HBA,
    HBA2_1,
    SMCB,
    SMCB2_1
}

@Serializable
data class Smartcard(
    val type: SmartcardType,
    val notBefore: String,
    val notAfter: String,
    val active: Boolean,
    val certificateRefs: List<Int>
)

private fun infereSmartcardFrom(entry: DirectoryEntry, index: Int, cert1: CertificateInfo, cert2: CertificateInfo? = null): Smartcard {
    val smartcardType = if (cert2 != null && entry.directoryEntryBase.personalEntry == true) {
        SmartcardType.HBA2_1
    } else if (cert2 != null) {
        SmartcardType.SMCB2_1
    } else if (entry.directoryEntryBase.personalEntry == true) {
        SmartcardType.HBA
    } else {
        SmartcardType.SMCB
    }

    return Smartcard(
        type = smartcardType,
        notBefore = cert1.notBefore,
        notAfter = cert1.notAfter,
        active = entry.userCertificates?.first { it.userCertificate?.certificateInfo?.serialNumber == cert1.serialNumber }?.active ?: false,
        certificateRefs = if (cert2 != null) listOf(index, index + 1) else listOf(index)
    )
}

val DirectoryEntry.smartcards: List<Smartcard>? get() {
    val entry = this
    return userCertificates
        ?.mapNotNull { it.userCertificate?.certificateInfo }
        ?.sortedBy { it.notBefore }
        ?.let {
            buildList<Smartcard> {
                var cert1: CertificateInfo? = null
                it.forEachIndexed { index, certInfo ->
                    if (cert1 == null) {
                        cert1 = certInfo
                    } else if (certInfo.publicKeyAlgorithm != cert1?.publicKeyAlgorithm) {
                        add(infereSmartcardFrom(entry, index - 1, cert1!!, certInfo))
                        cert1 = null
                    } else {
                        add(infereSmartcardFrom(entry, index - 1, cert1!!))
                        cert1 = certInfo
                    }
                }
                if (cert1 != null) {
                    add(infereSmartcardFrom(entry, it.size - 1, cert1!!))
                }
            }
        }
}

@Serializable
data class DirectoryEntryExt(
    @SerialName("DirectoryEntryBase")
    val directoryEntryBase: BaseDirectoryEntry,
    val userCertificates: List<UserCertificate>? = null,
    @SerialName("Fachdaten")
    val fachdaten: List<Fachdaten>? = null,
    val smartcards: List<Smartcard>? = null,
    val validationResult: SerializableValidationResult? = null,
    val kind: DirectoryEntryKind
)

object DirectoryEntryExtSerializer : KSerializer<DirectoryEntry> {

    override val descriptor: SerialDescriptor = DirectoryEntryExt.serializer().descriptor

    override fun serialize(encoder: Encoder, value: DirectoryEntry) {
        val surrogate = DirectoryEntryExt(
            directoryEntryBase = value.directoryEntryBase,
            userCertificates = value.userCertificates,
            fachdaten = value.fachdaten,
            smartcards = value.smartcards,
            kind = value.kind
        )
        encoder.encodeSerializableValue(DirectoryEntryExt.serializer(), surrogate)
    }

    override fun deserialize(decoder: Decoder): DirectoryEntry {
        val surrogate = decoder.decodeSerializableValue(DirectoryEntryExt.serializer())
        return DirectoryEntry(
            directoryEntryBase = surrogate.directoryEntryBase,
            userCertificates = surrogate.userCertificates,
            fachdaten = surrogate.fachdaten
        )
    }
}
