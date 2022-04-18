package vzd.admin.pki

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.serialization.Serializable
import mu.KotlinLogging
import org.bouncycastle.cert.jcajce.JcaX509CertificateHolder
import org.bouncycastle.cert.ocsp.*
import org.bouncycastle.operator.DigestCalculator
import org.bouncycastle.operator.jcajce.JcaDigestCalculatorProviderBuilder
import java.nio.channels.UnresolvedAddressException


private val logger = KotlinLogging.logger {}

enum class OCSPResponseCertificateStatus {
    GOOD, REVOKED, UNKNOWN, ERROR
}

@Serializable
data class OCSPResponse(var status: OCSPResponseCertificateStatus, var message: String? = null)

class PKIClient (block: Configuration.() -> Unit = {}) {
    class Configuration {
        var httpProxyURL: String? = null
    }

    private val config: Configuration = Configuration()
    private val httpClient: HttpClient

    private val digestCalculatorProviderBuilder = JcaDigestCalculatorProviderBuilder()
    private val digestCalculatorProvider = digestCalculatorProviderBuilder.build()
    private val digestCalculator: DigestCalculator = digestCalculatorProvider.get(CertificateID.HASH_SHA1)

    init {
        block(this.config)
        this.httpClient = HttpClient(CIO) {
            engine {
                config.httpProxyURL?.let {
                    logger.debug { "PKI client is using proxy: $it" }
                    proxy = ProxyBuilder.http(it)
                }
            }

            val l = logger

            install(Logging) {
                logger = Logger.DEFAULT
                if (l.isDebugEnabled) {
                    level = LogLevel.INFO
                } else {
                    level = LogLevel.NONE
                }
            }
        }
    }

    private val tsl: TrustedServiceListCache by lazy {
        TrustedServiceListCache.load() ?: run {
            val cache = TrustedServiceListCache(
                TSLLoader(httpClient).load(TrustEnvironment.TU),
                TSLLoader(httpClient).load(TrustEnvironment.RU),
                TSLLoader(httpClient).load(TrustEnvironment.PU),
            )
            TrustedServiceListCache.save(cache)
            cache
        }
    }

    suspend fun ocsp(eeCertDER: CertificateDataDER): OCSPResponse {
        try {
            val eeCert = eeCertDER.certificate
            logger.debug { "Looking for CA Certificate for ${eeCert.issuerDN}" }
            val issuerCert =
                tsl.caServices.first { it.caCertificate.certificate.subjectDN == eeCert.issuerDN }.caCertificate.certificate
            logger.info { "Verifying '${eeCert.subjectDN}' from '${issuerCert.subjectDN}' using OCSP Responder: '${eeCertDER.ocspResponderURL}'" }

            val certificateID = CertificateID(digestCalculator, JcaX509CertificateHolder(issuerCert),
                eeCert.serialNumber)

            val builder = OCSPReqBuilder()
            builder.addRequest(certificateID)
            val ocspReq = builder.build()

            val response = httpClient.post(eeCertDER.ocspResponderURL) {
                headers {
                    append(HttpHeaders.ContentType, "application/ocsp-request")
                }
                setBody(ocspReq.encoded)
            }

            val body: ByteArray = response.body()

            val basicOcspResp = OCSPResp(body).responseObject as BasicOCSPResp

            logger.debug { "Got OCSP Response extensions: ${basicOcspResp.extensionOIDs}" }

            val result = when (val certStatus = basicOcspResp.responses.first().certStatus) {
                CertificateStatus.GOOD -> OCSPResponse(OCSPResponseCertificateStatus.GOOD)
                is UnknownStatus -> OCSPResponse(OCSPResponseCertificateStatus.UNKNOWN,
                    "Certificate is unknown by the OCSP server")
                is RevokedStatus -> {
                    OCSPResponse(OCSPResponseCertificateStatus.REVOKED,
                        "Revocation reason: '${certStatus.revocationReason}' at ${certStatus.revocationTime}")
                }
                else -> OCSPResponse(OCSPResponseCertificateStatus.ERROR,
                    "Unknown status: $certStatus")
            }

            return result
        } catch (e: UnresolvedAddressException) {
            return OCSPResponse(OCSPResponseCertificateStatus.ERROR, "Unresolvable OCSP URL: ${eeCertDER.ocspResponderURL}")
        } catch (e: Throwable) {
            logger.error { e }
            return OCSPResponse(OCSPResponseCertificateStatus.ERROR, e.toString())
        }
    }

}

