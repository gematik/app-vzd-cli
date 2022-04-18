package vzd.teststuite.admin

import io.kotest.core.spec.style.FeatureSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldStartWith
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.logging.*
import mu.KotlinLogging
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.util.encoders.Base64
import vzd.admin.cli.escape
import vzd.admin.pki.*
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate

class TestCert : FeatureSpec({
    val localLogger = KotlinLogging.logger {}
    val httpClient: HttpClient by lazy {
        HttpClient(CIO) {
            install(Logging) {
                logger = Logger.DEFAULT
                level = LogLevel.INFO
            }
        }
    }


    feature("X509 Certificate Parsing") {
        scenario("Parse DER Certificate") {
            val certData =
                "MIIDgDCCAyegAwIBAgIHAaW81FFB9jAKBggqhkjOPQQDAjCBmTELMAkGA1UEBhMCREUxHzAdBgNVBAoMFmdlbWF0aWsgR21iSCBOT1QtVkFMSUQxSDBGBgNVBAsMP0luc3RpdHV0aW9uIGRlcyBHZXN1bmRoZWl0c3dlc2Vucy1DQSBkZXIgVGVsZW1hdGlraW5mcmFzdHJ1a3R1cjEfMB0GA1UEAwwWR0VNLlNNQ0ItQ0E5IFRFU1QtT05MWTAeFw0yMDAxMjcwMDAwMDBaFw0yNDEyMTEyMzU5NTlaMIHVMQswCQYDVQQGEwJERTERMA8GA1UEBwwIVMO2bm5pbmcxDjAMBgNVBBEMBTI1ODMyMRMwEQYDVQQJDApBbSBNYXJrdCAxMS0wKwYDVQQKDCRQcmF4aXMgTGlsbyBHcsOkZmluIGRlIEJvZXJOT1QtVkFMSUQxDTALBgNVBAQMBEJvZXIxDTALBgNVBCoMBExpbG8xEjAQBgNVBAwMCVByb2YuIERyLjEtMCsGA1UEAwwkUHJheGlzIExpbG8gR3LDpGZpbiBkZSBCb2VyVEVTVC1PTkxZMFowFAYHKoZIzj0CAQYJKyQDAwIIAQEHA0IABE+qn0H8KYaa4IszxE3FLWH9/V58z2iYu7hUVfe7PBOQKNpBw+c6wO710QhZFLr35Ks9GQGN2IBtpITcoWsR7ZKjggEZMIIBFTAdBgNVHQ4EFgQU48MFLtrb7L8kGQ1BZpNrDgGpDUEwDgYDVR0PAQH/BAQDAgMIMCAGA1UdIAQZMBcwCgYIKoIUAEwEgSMwCQYHKoIUAEwETDAfBgNVHSMEGDAWgBRiiJrE3vyj85M5y5+Q5xOaPYnMdTA4BggrBgEFBQcBAQQsMCowKAYIKwYBBQUHMAGGHGh0dHA6Ly9laGNhLmdlbWF0aWsuZGUvb2NzcC8wDAYDVR0TAQH/BAIwADBZBgUrJAgDAwRQME4wTDBKMEgwRjAWDBRCZXRyaWVic3N0w6R0dGUgQXJ6dDAJBgcqghQATAQyEyExLVNNQy1CLVRlc3RrYXJ0ZS04ODMxMTAwMDAxMTcwMzUwCgYIKoZIzj0EAwIDRwAwRAIge+TDJbeTVwj3bV78Vl9ycVZx5FhxkQYVBl6JoOwo6/wCID54Dvjk0aFAMutqYdKce00bQCaGRRzzm9Ck0dsqGfaK"
            val bytes = Base64.decode(certData)

            val cf = CertificateFactory.getInstance("X.509")
            val cert: X509Certificate = cf.generateCertificate(bytes.inputStream()) as X509Certificate

            cert.subjectX500Principal.name shouldStartWith "CN=Praxis Lilo Gräfin de BoerTEST-ONLY"

            val reencodedCertData = Base64.toBase64String(cert.encoded)

            reencodedCertData shouldBe certData

        }

        scenario("Escape specual characters in TelematikID") {
            val str = "9-243423434\n";

            str.escape() shouldBe "9-243423434\\n"
        }

        scenario("Parse complex DN") {

            val dn2 = "SURNAME=Äppler + GIVENNAME=Dominik-Peter Graf + SERIALNUMBER=80276883110000109354 + CN=Dominik-Peter ÄpplerTEST-ONLY, C=DE"

            val x500Name2 = X500Name(dn2)

            println(x500Name2.rdNs.map { it.typesAndValues.toList() }.flatten() )


        }
    }

    feature("TSL handling") {
        scenario("Load TSL from the internet") {
            val tsl = TrustedServiceListCache(pu = TSLLoader(httpClient).load(TrustEnvironment.PU))
            tsl.caServices.forEach{
                localLogger.debug { it.caCertificate.certificate.subjectDN.name }
            }

            val ca = tsl.caServices.first { it.env == TrustEnvironment.PU && it.name == "CN=D-Trust.SMCB-CA3,OU=Institution des Gesundheitswesens-CA der Telematikinfrastruktur,O=D-TRUST GmbH,C=DE"}
            ca shouldNotBe null
        }
    }

    feature("OCSP Requests") {
        val cert64 = "MIIDgDCCAyegAwIBAgIHAaW81FFB9jAKBggqhkjOPQQDAjCBmTELMAkGA1UEBhMCREUxHzAdBgNVBAoMFmdlbWF0aWsgR21iSCBOT1QtVkFMSUQxSDBGBgNVBAsMP0luc3RpdHV0aW9uIGRlcyBHZXN1bmRoZWl0c3dlc2Vucy1DQSBkZXIgVGVsZW1hdGlraW5mcmFzdHJ1a3R1cjEfMB0GA1UEAwwWR0VNLlNNQ0ItQ0E5IFRFU1QtT05MWTAeFw0yMDAxMjcwMDAwMDBaFw0yNDEyMTEyMzU5NTlaMIHVMQswCQYDVQQGEwJERTERMA8GA1UEBwwIVMO2bm5pbmcxDjAMBgNVBBEMBTI1ODMyMRMwEQYDVQQJDApBbSBNYXJrdCAxMS0wKwYDVQQKDCRQcmF4aXMgTGlsbyBHcsOkZmluIGRlIEJvZXJOT1QtVkFMSUQxDTALBgNVBAQMBEJvZXIxDTALBgNVBCoMBExpbG8xEjAQBgNVBAwMCVByb2YuIERyLjEtMCsGA1UEAwwkUHJheGlzIExpbG8gR3LDpGZpbiBkZSBCb2VyVEVTVC1PTkxZMFowFAYHKoZIzj0CAQYJKyQDAwIIAQEHA0IABE+qn0H8KYaa4IszxE3FLWH9/V58z2iYu7hUVfe7PBOQKNpBw+c6wO710QhZFLr35Ks9GQGN2IBtpITcoWsR7ZKjggEZMIIBFTAdBgNVHQ4EFgQU48MFLtrb7L8kGQ1BZpNrDgGpDUEwDgYDVR0PAQH/BAQDAgMIMCAGA1UdIAQZMBcwCgYIKoIUAEwEgSMwCQYHKoIUAEwETDAfBgNVHSMEGDAWgBRiiJrE3vyj85M5y5+Q5xOaPYnMdTA4BggrBgEFBQcBAQQsMCowKAYIKwYBBQUHMAGGHGh0dHA6Ly9laGNhLmdlbWF0aWsuZGUvb2NzcC8wDAYDVR0TAQH/BAIwADBZBgUrJAgDAwRQME4wTDBKMEgwRjAWDBRCZXRyaWVic3N0w6R0dGUgQXJ6dDAJBgcqghQATAQyEyExLVNNQy1CLVRlc3RrYXJ0ZS04ODMxMTAwMDAxMTcwMzUwCgYIKoZIzj0EAwIDRwAwRAIge+TDJbeTVwj3bV78Vl9ycVZx5FhxkQYVBl6JoOwo6/wCID54Dvjk0aFAMutqYdKce00bQCaGRRzzm9Ck0dsqGfaK"
        val pkiClient = PKIClient {

        }
        val certDER = CertificateDataDER(cert64)
        val ocspResponse = pkiClient.ocsp(certDER)
        ocspResponse.status shouldBe OCSPResponseCertificateStatus.GOOD

    }
})
