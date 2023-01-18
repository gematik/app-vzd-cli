package de.gematik.ti.directory.pki

import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import org.bouncycastle.asn1.ASN1Encodable
import org.bouncycastle.asn1.ASN1ObjectIdentifier
import org.bouncycastle.asn1.isismtt.ISISMTTObjectIdentifiers
import org.bouncycastle.asn1.isismtt.x509.AdmissionSyntax
import org.bouncycastle.asn1.x500.AttributeTypeAndValue
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.asn1.x500.style.BCStyle
import org.bouncycastle.asn1.x500.style.IETFUtils
import org.bouncycastle.asn1.x509.AuthorityInformationAccess
import org.bouncycastle.asn1.x509.X509ObjectIdentifiers
import org.bouncycastle.cert.X509CertificateHolder
import org.bouncycastle.util.encoders.Base64
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate

private val timeZone = TimeZone.of("Europe/Berlin")

/**
 * Simple datatype for base64 encoded certificates to differentiate them from plain strings
 */
@Serializable(with = CertificateDataDERSerializer::class)
data class CertificateDataDER(val base64String: String, val _certInfo: CertificateInfo? = null) {
    val certificateInfo by lazy {
        _certInfo ?: run {

            val keyUsage = mutableListOf<String>()
            certificate.keyUsage?.forEachIndexed { index, element ->
                when (index) {
                    0 -> if (element) keyUsage.add("digitalSignature") // digitalSignature        (0)
                    1 -> if (element) keyUsage.add("nonRepudiation") // nonRepudiation          (1)
                    2 -> if (element) keyUsage.add("keyEncipherment") // keyEncipherment         (2)
                    3 -> if (element) keyUsage.add("dataEncipherment") // dataEncipherment        (3)
                    4 -> if (element) keyUsage.add("keyAgreement") // keyAgreement            (4)
                    5 -> if (element) keyUsage.add("keyCertSign") // keyCertSign             (5)
                    6 -> if (element) keyUsage.add("cRLSign") // cRLSign                 (6)
                    7 -> if (element) keyUsage.add("encipherOnly") // encipherOnly            (7)
                    8 -> if (element) keyUsage.add("decipherOnly") // decipherOnly            (8)
                }
            }

            val admission = Admission(certificate)
            val admissionInfo = AdmissionStatementInfo(
                admissionAuthority = admission.admissionAuthority,
                professionItems = admission.professionItems,
                professionOids = admission.professionOids,
                registrationNumber = admission.registrationNumber,
            )

            CertificateInfo(
                certificate.subjectX500Principal.name,
                NameInfo(X500Name(certificate.subjectX500Principal.name)),
                certificate.issuerX500Principal.name,
                certificate.sigAlgName,
                certificate.publicKey.algorithm,
                certificate.serialNumber.toString(),
                keyUsage,
                Instant.fromEpochMilliseconds(certificate.notBefore.time),
                Instant.fromEpochMilliseconds(certificate.notAfter.time),
                admissionInfo,
                base64String,
                ocspResponderURL,
            )
        }
    }

    val certificate by lazy {
        val bytes = Base64.decode(base64String)
        val cf = CertificateFactory.getInstance("X.509")
        cf.generateCertificate(bytes.inputStream()) as X509Certificate
    }

    val ocspResponderURL by lazy {
        try {
            val certHolder = X509CertificateHolder(Base64.decode(base64String))

            val aiaExtension = AuthorityInformationAccess.fromExtensions(certHolder.extensions)

            if (aiaExtension != null && aiaExtension.accessDescriptions != null) {
                aiaExtension.accessDescriptions.asSequence()
                    .filter { ad -> ad.accessMethod == X509ObjectIdentifiers.id_ad_ocsp }
                    .map { ad -> ad.accessLocation.name }
                    .first().toASN1Primitive().toString()
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
}

/**
 * Serializes {CertificateDataDER} to primitive string.
 */
object CertificateDataDERSerializer : KSerializer<CertificateDataDER> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("CertificateDataDER", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: CertificateDataDER) {
        encoder.encodeString(value.base64String)
    }

    override fun deserialize(decoder: Decoder): CertificateDataDER {
        return CertificateDataDER(decoder.decodeString())
    }
}

/**
 * Special Serializer to display the textual summary of the X509Certificate
 */
object ExtendedCertificateDataDERSerializer : KSerializer<CertificateDataDER> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("CertificateDataDER", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: CertificateDataDER) {
        val surrogate = value.certificateInfo
        encoder.encodeSerializableValue(CertificateInfo.serializer(), surrogate)
    }

    override fun deserialize(decoder: Decoder): CertificateDataDER {
        val surrogate: CertificateInfo = decoder.decodeSerializableValue(CertificateInfo.serializer())
        return CertificateDataDER(surrogate.certData, surrogate)
    }
}

/**
 * Structured X500Name information
 */
@Serializable
data class NameInfo(
    var cn: String? = null,
    var givenName: String? = null,
    var sn: String? = null,
    var title: String? = null,
    var serialNumber: String? = null,
    var streetAddress: String? = null,
    var postalCode: String? = null,
    var localityName: String? = null,
    var stateOrProvinceName: String? = null,
    var countryCode: String? = null,
) {
    constructor(x500Name: X500Name) : this() {
        val rdns = x500Name.rdNs.map { it.typesAndValues.toList() }.flatten().toTypedArray()
        this.cn = valueOf(rdns, BCStyle.CN)
        this.sn = valueOf(rdns, BCStyle.SURNAME)
        this.givenName = valueOf(rdns, BCStyle.GIVENNAME)
        this.title = valueOf(rdns, BCStyle.T)
        this.serialNumber = valueOf(rdns, BCStyle.SERIALNUMBER)
        this.streetAddress = valueOf(rdns, BCStyle.STREET)
        this.postalCode = valueOf(rdns, BCStyle.POSTAL_CODE)
        this.localityName = valueOf(rdns, BCStyle.L)
        this.stateOrProvinceName = valueOf(rdns, BCStyle.ST)
        this.countryCode = valueOf(rdns, BCStyle.C)
    }

    private fun valueOf(rdns: Array<AttributeTypeAndValue>, type: ASN1ObjectIdentifier): String? {
        val value = rdns.firstOrNull { it.type == type }?.value
        return value?.let {
            IETFUtils.valueToString(value)
        } ?: run {
            null
        }
    }
}

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
 * Textual information about the X509 Certificate
 */
@Serializable
data class CertificateInfo(
    val subject: String,
    val subjectInfo: NameInfo,
    val issuer: String,
    val signatureAlgorithm: String,
    val publicKeyAlgorithm: String,
    val serialNumber: String,
    val keyUsage: List<String>,
    val notBefore: Instant,
    val notAfter: Instant,
    val admissionStatement: AdmissionStatementInfo,
    val certData: String,
    val ocspReponderURL: String? = null,
    var ocspResponse: OCSPResponse? = null,
)

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
