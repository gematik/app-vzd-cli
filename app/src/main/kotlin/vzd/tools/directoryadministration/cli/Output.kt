package vzd.tools.directoryadministration.cli

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encodeToString
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import net.mamoe.yamlkt.Yaml
import org.bouncycastle.util.encoders.Base64
import vzd.tools.directoryadministration.CertificateDataDER
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate

inline fun <reified E>printYaml(value: E?, showRawCert: Boolean) {
    println(Yaml {
        if (!showRawCert) {
            serializersModule = printerSerializersModule
        }
    }.encodeToString(value))
}

inline fun <reified E>printJson(value: E?, showRawCert: Boolean) {
    println(Json {
        prettyPrint = true
        if (!showRawCert) {
            serializersModule = printerSerializersModule
        }
    }.encodeToString(value))
}

@Serializable
@SerialName("CertificateDataDER")
data class CertificateDataDERSurrogate (
    val subject: String,
    val issuer: String,
    val signatureAlgorithm: String,
    val publicKeyAlgorithm: String
) {
    companion object Factory {
        fun convert(base64String: String): CertificateDataDERSurrogate {
            val bytes = Base64.decode(base64String)
            val cf = CertificateFactory.getInstance("X.509")
            val cert: X509Certificate = cf.generateCertificate(bytes.inputStream()) as X509Certificate

            cert.publicKey.algorithm

            return CertificateDataDERSurrogate(
                cert.subjectDN.name,
                cert.issuerDN.name,
                cert.sigAlgName,
                cert.publicKey.algorithm
            )
        }

    }
}

object CertificateDataDERPrinterSerializer : KSerializer<CertificateDataDER> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("CertificateDataDER", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: CertificateDataDER) {
        val surrogate = CertificateDataDERSurrogate.Factory.convert(value.base64String)
        encoder.encodeSerializableValue(CertificateDataDERSurrogate.serializer(), surrogate);
    }

    override fun deserialize(decoder: Decoder): CertificateDataDER {
        return CertificateDataDER(decoder.decodeString())
    }
}