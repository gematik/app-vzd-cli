package vzd.admin.client

import kotlinx.serialization.Serializable
import mu.KotlinLogging
import org.bouncycastle.asn1.ASN1Encodable
import org.bouncycastle.asn1.isismtt.ISISMTTObjectIdentifiers
import org.bouncycastle.asn1.isismtt.x509.AdmissionSyntax
import org.bouncycastle.cert.X509CertificateHolder
import org.bouncycastle.util.encoders.Base64
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*

private val logger = KotlinLogging.logger {}

/**
 * Information about the admisstionStatement in the X509 Certificate
 */
@Serializable
data class AdmissionStatementInfo(
    val admissionAuthority: String,
    val professionItems: List<String>,
    val professionOids: List<String>,
    val registrationNumber: String,
)

/**
 * Textual information about the C509 Certificate
 */
@Serializable
data class CertificateInfo(
    val subject: String,
    val issuer: String,
    val signatureAlgorithm: String,
    val publicKeyAlgorithm: String,
    val serialNumber: String,
    val keyUsage: List<String>,
    val notBefore: String,
    val notAfter: String,
    val admissionStatement: AdmissionStatementInfo,
    val certData: String,
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

    val admission = Admission(cert)
    val admissionInfo = AdmissionStatementInfo(
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
        base64String
    )
}

/**
 * Port of gematik Java class to Kotlin.
 */
class Admission(x509EeCert: X509Certificate) {
    private val asn1Admission: ASN1Encodable

    init {
        asn1Admission = X509CertificateHolder(x509EeCert.encoded)
            .extensions
            .getExtensionParsedValue(ISISMTTObjectIdentifiers.id_isismtt_at_admission)
    }

    /**
     * Reading admission authority
     *
     * @return String of the admission authority or an empty string if not present
     */
    val admissionAuthority: String
        get() {
            return try {
                AdmissionSyntax.getInstance(asn1Admission).admissionAuthority.name.toString()
            } catch (e: NullPointerException) {
                ""
            }
        }

    /**
     * Reading profession items
     *
     * @return Non duplicate list of profession items of the first profession info of the first admission in the certificate
     */
    val professionItems: List<String>
        get() {
            return AdmissionSyntax.getInstance(asn1Admission).contentsOfAdmissions[0].professionInfos.map {
                it.professionItems.map {
                    it.string
                }
            }.flatten()
        }

    /**
     * Reading profession oid's
     *
     * @return Non duplicate list of profession oid's of the first profession info of the first admission in the certificate
     */
    val professionOids: List<String>
        get() {

            return AdmissionSyntax.getInstance(asn1Admission).contentsOfAdmissions[0].professionInfos.map {
                it.professionOIDs.map {
                    it.id
                }
            }.flatten()

        }

    /**
     * Reading registration number
     *
     * @return String of the registration number of the first profession info of the first admission in the certificate
     */
    val registrationNumber: String
        get() {
            return AdmissionSyntax.getInstance(asn1Admission).contentsOfAdmissions[0].professionInfos[0].registrationNumber
        }
}
