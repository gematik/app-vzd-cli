package vzd.tools.directoryadministration.cli

import de.gematik.pki.certificate.Admission
import kotlinx.serialization.*
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import net.mamoe.yamlkt.Yaml
import org.bouncycastle.util.encoders.Base64
import vzd.tools.directoryadministration.CertificateDataDER
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*

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
data class AdmissionSurrogate(
    val admissionAuthority: String,
    val professionItems: Set<String>,
    val professionOids: Set<String>,
    val registrationNumber: String
)

@Serializable
@SerialName("CertificateDataDER")
data class CertificateDataDERSurrogate (
    val subject: String,
    val issuer: String,
    val signatureAlgorithm: String,
    val publicKeyAlgorithm: String,
    val serialNumber: String,
    val keyUsage: List<String>,
    val notBefore: String,
    val notAfter: String,
    val admission: AdmissionSurrogate,
) {
    companion object Factory {
        fun convert(base64String: String): CertificateDataDERSurrogate {
            val bytes = Base64.decode(base64String)
            val cf = CertificateFactory.getInstance("X.509")
            val cert: X509Certificate = cf.generateCertificate(bytes.inputStream()) as X509Certificate

            val keyUsage = mutableListOf<String>()

            /*
                KeyUsage ::= BIT STRING {
                   digitalSignature        (0),
                   nonRepudiation          (1),
                   keyEncipherment         (2),
                   dataEncipherment        (3),
                   keyAgreement            (4),
                   keyCertSign             (5),
                   cRLSign                 (6),
                   encipherOnly            (7),
                   decipherOnly            (8) }
             */
            cert.keyUsage?.forEachIndexed { index, element ->
                when (index) {
                    0 -> if (element) keyUsage.add("digitalSignature")
                    1 -> if (element) keyUsage.add("nonRepudiation")
                    2 -> if (element) keyUsage.add("keyEncipherment")
                    3 -> if (element) keyUsage.add("dataEncipherment")
                    4 -> if (element) keyUsage.add("keyAgreement")
                    5 -> if (element) keyUsage.add("keyCertSign")
                    6 -> if (element) keyUsage.add("cRLSign")
                    7 -> if (element) keyUsage.add("encipherOnly")
                    8 -> if (element) keyUsage.add("decipherOnly")
                }
            }

            fun dateToString(date: Date): String {
                return DateTimeFormatter.ISO_DATE_TIME.format(date.toInstant()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDateTime())
            }

            var admission = Admission(cert)
            var admissionSurrogate = AdmissionSurrogate(
                admissionAuthority = admission.admissionAuthority,
                professionItems = admission.professionItems,
                professionOids = admission.professionOids,
                registrationNumber = admission.registrationNumber
            )

            return CertificateDataDERSurrogate(
                cert.subjectDN.name,
                cert.issuerDN.name,
                cert.sigAlgName,
                cert.publicKey.algorithm,
                serialNumber = cert.serialNumber.toString(),
                keyUsage = keyUsage,
                dateToString(cert.notBefore),
                dateToString(cert.notAfter),
                admissionSurrogate,
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