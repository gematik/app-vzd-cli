package de.gematik.ti.directory.pki

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.util.*
import kotlinx.serialization.Serializable
import mu.KotlinLogging
import org.bouncycastle.asn1.isismtt.ISISMTTObjectIdentifiers
import org.bouncycastle.asn1.isismtt.ocsp.CertHash
import org.bouncycastle.cert.jcajce.JcaX509CertificateHolder
import org.bouncycastle.cert.ocsp.*
import org.bouncycastle.operator.DigestCalculator
import org.bouncycastle.operator.jcajce.JcaDigestCalculatorProviderBuilder
import java.nio.channels.UnresolvedAddressException
import java.security.MessageDigest

private val logger = KotlinLogging.logger {}

enum class OCSPResponseCertificateStatus {
    GOOD,
    REVOKED,
    UNKNOWN,
    CERT_HASH_ERROR,
    ERROR,
}

fun loadAllEnvironments(httpClient: HttpClient): ListOfTrustedServiceLists =
    ListOfTrustedServiceLists(
        tu = ListOfTrustedServiceLists.loadFromServer(httpClient, TrustEnvironment.TU),
        ru = ListOfTrustedServiceLists.loadFromServer(httpClient, TrustEnvironment.RU),
        pu = ListOfTrustedServiceLists.loadFromServer(httpClient, TrustEnvironment.PU),
    )

@Serializable
data class OCSPResponse(
    var status: OCSPResponseCertificateStatus,
    var message: String? = null
)

class PKIClient(
    block: Configuration.() -> Unit = {}
) {
    class Configuration {
        var httpProxyURL: String? = null
        var loader: ((HttpClient) -> ListOfTrustedServiceLists) = { ListOfTrustedServiceLists() }
    }

    private val config: Configuration = Configuration()
    private val httpClient: HttpClient
    private val loader: ((HttpClient) -> ListOfTrustedServiceLists)

    private val digestCalculatorProviderBuilder = JcaDigestCalculatorProviderBuilder()
    private val digestCalculatorProvider = digestCalculatorProviderBuilder.build()
    private val digestCalculator: DigestCalculator = digestCalculatorProvider.get(CertificateID.HASH_SHA1)

    init {
        block(this.config)
        this.httpClient =
            HttpClient(CIO) {
                engine {
                    config.httpProxyURL?.let {
                        logger.debug { "PKI client is using proxy: $it" }
                        proxy = ProxyBuilder.http(it)
                    }
                }

                val l = logger

                install(Logging) {
                    logger = Logger.DEFAULT
                    level =
                        if (l.isDebugEnabled) {
                            LogLevel.INFO
                        } else {
                            LogLevel.NONE
                        }
                }
            }
        this.loader = this.config.loader
    }

    val tsl: ListOfTrustedServiceLists by lazy {
        loader(httpClient)
    }

    suspend fun ocsp(eeCertDER: CertificateDataDER): OCSPResponse {
        val ocspResponderURL =
            eeCertDER.ocspResponderURL ?: run {
                logger.error { "Certificate has no OCSP URL: ${eeCertDER.certificateInfo.subject}" }
                return OCSPResponse(OCSPResponseCertificateStatus.ERROR, "Certificate has no OCSP URL")
            }
        try {
            val eeCert = eeCertDER.certificate
            logger.debug { "Looking for CA Certificate for ${eeCert.issuerX500Principal}" }
            val issuerCert =
                tsl.caServices
                    .first { it.caCertificate.certificate.subjectX500Principal == eeCert.issuerX500Principal }
                    .caCertificate.certificate

            logger.info {
                "Verifying '${eeCert.subjectX500Principal}' from '${issuerCert.subjectX500Principal}' using OCSP Responder: '$ocspResponderURL'"
            }

            val certificateID =
                CertificateID(
                    digestCalculator,
                    JcaX509CertificateHolder(issuerCert),
                    eeCert.serialNumber,
                )

            val builder = OCSPReqBuilder()
            builder.addRequest(certificateID)
            val ocspReq = builder.build()

            logger.debug { "Verifying OCSP serialNumber: ${ocspReq.requestList[0].certID.serialNumber}, responder: $ocspResponderURL" }
            val response =
                httpClient.post(ocspResponderURL) {
                    headers {
                        append(HttpHeaders.ContentType, "application/ocsp-request")
                    }
                    setBody(ocspReq.encoded)
                }

            if (response.status != HttpStatusCode.OK) {
                throw Exception("OCSP server returned bad response: \n${response.body<String>()}")
            }

            val body: ByteArray = response.body()

            val basicOcspResp = OCSPResp(body).responseObject as BasicOCSPResp
            val ocspResp = basicOcspResp.responses.first()

            val result =
                when (val certStatus = ocspResp.certStatus) {
                    CertificateStatus.GOOD -> {
                        if (ocspResp.getExtension(ISISMTTObjectIdentifiers.id_isismtt_at_certHash) == null) {
                            logger.error { "Cert hash extension is missing in response" }
                            return OCSPResponse(OCSPResponseCertificateStatus.CERT_HASH_ERROR)
                        }
                        val asn1CertHash =
                            CertHash.getInstance(
                                ocspResp.getExtension(ISISMTTObjectIdentifiers.id_isismtt_at_certHash).parsedValue,
                            )
                        val digest = MessageDigest.getInstance("SHA-256")

                        if (!asn1CertHash.certificateHash.contentEquals(digest.digest(eeCert.encoded))) {
                            logger.error {
                                "Cert hash does not match ${asn1CertHash.certificateHash.encodeBase64()} != ${digest.digest(
                                    eeCert.encoded,
                                ).encodeBase64()}"
                            }
                            return OCSPResponse(OCSPResponseCertificateStatus.CERT_HASH_ERROR)
                        } else {
                            logger.info { "Cert hash matches: ${asn1CertHash.certificateHash.encodeBase64()}" }
                        }
                        return OCSPResponse(OCSPResponseCertificateStatus.GOOD)
                    }
                    is UnknownStatus -> {
                        logger.info {
                            "Certificate is unknown by the OCSP server subject='${eeCert.subjectX500Principal}', serialNumber='${ocspReq.requestList[0].certID.serialNumber}', issuer='${issuerCert.subjectX500Principal}'"
                        }
                        OCSPResponse(
                            OCSPResponseCertificateStatus.UNKNOWN,
                            "Certificate is unknown by the OCSP server",
                        )
                    }
                    is RevokedStatus -> {
                        val reason = if (certStatus.hasRevocationReason()) certStatus.revocationReason else "none"
                        OCSPResponse(
                            OCSPResponseCertificateStatus.REVOKED,
                            "Revocation reason: '$reason' at ${certStatus.revocationTime}",
                        )
                    }
                    else ->
                        OCSPResponse(
                            OCSPResponseCertificateStatus.ERROR,
                            "Unknown status: $certStatus",
                        )
                }

            return result
        } catch (e: UnresolvedAddressException) {
            return OCSPResponse(OCSPResponseCertificateStatus.ERROR, "Unresolvable OCSP URL: $ocspResponderURL")
        } catch (e: Throwable) {
            logger.error(e) {}
            return OCSPResponse(OCSPResponseCertificateStatus.ERROR, e.message)
        }
    }
}
