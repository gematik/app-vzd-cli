package vzd.tools.directoryadministration

import de.gematik.pki.certificate.Admission
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import org.bouncycastle.util.encoders.Base64
import java.lang.UnsupportedOperationException
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*

/**
 * Information about the admisstionStatement in the X509 Certificate
 */
@Serializable
data class AdmissionStatementInfo (
    val admissionAuthority: String,
    val professionItems: Set<String>,
    val professionOids: Set<String>,
    val registrationNumber: String
)

/**
 * Textual information about the C509 Certificate
 */
@Serializable
data class CertificateInfo (
    val subject: String,
    val issuer: String,
    val signatureAlgorithm: String,
    val publicKeyAlgorithm: String,
    val serialNumber: String,
    val keyUsage: List<String>,
    val notBefore: String,
    val notAfter: String,
    val admissionStatement: AdmissionStatementInfo,
)

/**
 * Converts the base64 encoded DER data structure to X509Certificate object.
 */
fun CertificateDataDER.toX509Certificate(): X509Certificate {
    val bytes = Base64.decode(base64String)
    val cf = CertificateFactory.getInstance("X.509")
    return cf.generateCertificate(bytes.inputStream()) as X509Certificate
}

/**
 * Converts the base64 encoded DER data structure to CertificateInfo object.
 */
fun CertificateDataDER.toCertificateInfo(): CertificateInfo {
    val cert = toX509Certificate()
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
    var admissionInfo = AdmissionStatementInfo(
        admissionAuthority = admission.admissionAuthority,
        professionItems = admission.professionItems,
        professionOids = admission.professionOids,
        registrationNumber = admission.registrationNumber
    )

    return CertificateInfo(
        cert.subjectDN.name,
        cert.issuerDN.name,
        cert.sigAlgName,
        cert.publicKey.algorithm,
        cert.serialNumber.toString(),
        keyUsage,
        dateToString(cert.notBefore),
        dateToString(cert.notAfter),
        admissionInfo,
    )
}

/**
 * Special Serializer to display the textual summary of the X509Certificate
 */
object CertificateDataDERInfoSerializer : KSerializer<CertificateDataDER> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("CertificateDataDER", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: CertificateDataDER) {
        val surrogate = value.toCertificateInfo()
        encoder.encodeSerializableValue(CertificateInfo.serializer(), surrogate);
    }

    override fun deserialize(decoder: Decoder): CertificateDataDER {
        throw UnsupportedOperationException()
    }
}