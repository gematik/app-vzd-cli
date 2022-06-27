package vzd.ldif

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import vzd.pki.*
import java.util.*

/**
 * Special Serializer to display the textual summary of the X509Certificate
 */
object CertificateDataDERInfoSerializer : KSerializer<CertificateDataDER> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("CertificateDataDER", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: CertificateDataDER) {
        val surrogate = CertificateInfo(
            subject = HIDDEN_VALUE,
            subjectInfo = NameInfo(cn = HIDDEN_VALUE),
            issuer = HIDDEN_VALUE,
            signatureAlgorithm = HIDDEN_VALUE,
            publicKeyAlgorithm = HIDDEN_VALUE,
            serialNumber = HIDDEN_VALUE,
            keyUsage = listOf(HIDDEN_VALUE),
            notBefore = dateToString(Date(0L)),
            notAfter = dateToString(Date(0L)),
            admissionStatement = AdmissionStatementInfo(
                admissionAuthority = HIDDEN_VALUE,
                professionItems = listOf(HIDDEN_VALUE),
                professionOids = listOf(HIDDEN_VALUE),
                registrationNumber = HIDDEN_VALUE
            ),
            certData = value.base64String
        )
        encoder.encodeSerializableValue(CertificateInfo.serializer(), surrogate)
    }

    override fun deserialize(decoder: Decoder): CertificateDataDER {
        throw UnsupportedOperationException()
    }
}

val customSerializersModule = SerializersModule {
    contextual(CertificateDataDERInfoSerializer)
}
