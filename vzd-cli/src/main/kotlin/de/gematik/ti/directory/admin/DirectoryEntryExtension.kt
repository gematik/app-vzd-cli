package de.gematik.ti.directory.admin

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

val DirectoryEntry.kind: DirectoryEntryKind get() {
    return DirectoryEntryKind.values().first {
        it.matcher.invoke(this)
    }
}

@Serializable
data class DirectoryEntryExtension(
    @SerialName("DirectoryEntryBase")
    var directoryEntryBase: BaseDirectoryEntry,
    var userCertificates: List<UserCertificate>? = null,
    @SerialName("Fachdaten")
    var fachdaten: List<Fachdaten>? = null,
    var kind: DirectoryEntryKind
)

object DirectoryEntryExtSerializer : KSerializer<DirectoryEntry> {

    override val descriptor: SerialDescriptor = DirectoryEntryExtension.serializer().descriptor

    override fun serialize(encoder: Encoder, value: DirectoryEntry) {
        val surrogate = DirectoryEntryExtension(
            directoryEntryBase = value.directoryEntryBase,
            userCertificates = value.userCertificates,
            fachdaten = value.fachdaten,
            kind = value.kind
        )
        encoder.encodeSerializableValue(DirectoryEntryExtension.serializer(), surrogate)
    }

    override fun deserialize(decoder: Decoder): DirectoryEntry {
        val surrogate = decoder.decodeSerializableValue(DirectoryEntryExtension.serializer())
        return DirectoryEntry(
            directoryEntryBase = surrogate.directoryEntryBase,
            userCertificates = surrogate.userCertificates,
            fachdaten = surrogate.fachdaten
        )
    }
}
