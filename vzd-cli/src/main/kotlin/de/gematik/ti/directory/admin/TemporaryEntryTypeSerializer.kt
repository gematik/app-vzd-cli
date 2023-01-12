package de.gematik.ti.directory.admin

import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * Temporary serializer for interoperability between VZD 3.1 and 3.2
 */
object TemporaryEntryTypeSerializer : KSerializer<Int?> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("EntryType", PrimitiveKind.INT)
    private val intListSerializer: KSerializer<List<Int>> = ListSerializer(Int.serializer())

    override fun serialize(encoder: Encoder, value: Int?) {
        if (value != null) {
            encoder.encodeInt(value)
        }
    }

    override fun deserialize(decoder: Decoder): Int? {
        return try {
            decoder.decodeInt()
        } catch (e: Throwable) {
            try {
                decoder.decodeSerializableValue(intListSerializer).first()
            } catch (ee: Throwable) {
                decoder.decodeNull()
            }
        }
    }
}
