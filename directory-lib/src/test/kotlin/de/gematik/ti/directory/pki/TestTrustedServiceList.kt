import de.gematik.ti.directory.pki.*
import io.kotest.core.spec.style.FeatureSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.ktor.client.*
import io.ktor.client.engine.cio.*

class TestTrustedServiceList : FeatureSpec({

    feature("Pluggable TSL loader") {
        scenario("Loader that always delivers 0 CAs") {
            val pkiClient =
                PKIClient {
                    loader = {
                        ListOfTrustedServiceLists()
                    }
                }

            pkiClient.tsl.caServices.size shouldBe 0
        }
        scenario("Loader only for TU") {
            val pkiClient =
                PKIClient {
                    loader = { httpClient ->
                        ListOfTrustedServiceLists(tu = ListOfTrustedServiceLists.loadFromServer(httpClient, TrustEnvironment.TU))
                    }
                }

            pkiClient.tsl.caServices.size shouldNotBe 0
            pkiClient.tsl.caServices.firstOrNull { it -> it.env == TrustEnvironment.TU } shouldNotBe null
            pkiClient.tsl.caServices.firstOrNull { it -> it.env != TrustEnvironment.TU } shouldBe null
        }
    }

    feature("TSL handling") {
        scenario("Load TSL from the internet") {
            val tsl = ListOfTrustedServiceLists(pu = ListOfTrustedServiceLists.loadFromServer(HttpClient(CIO), TrustEnvironment.PU))
            tsl.caServices.size shouldNotBe 0
            val ca =
                tsl.caServices.first {
                    it.env == TrustEnvironment.PU && it.name == "CN=D-Trust.SMCB-CA3,OU=Institution des Gesundheitswesens-CA der Telematikinfrastruktur,O=D-TRUST GmbH,C=DE"
                }
            ca shouldNotBe null
        }
    }

    feature("OCSP Requests") {
        val cert64 = "MIIDgDCCAyegAwIBAgIHAaW81FFB9jAKBggqhkjOPQQDAjCBmTELMAkGA1UEBhMCREUxHzAdBgNVBAoMFmdlbWF0aWsgR21iSCBOT1QtVkFMSUQxSDBGBgNVBAsMP0luc3RpdHV0aW9uIGRlcyBHZXN1bmRoZWl0c3dlc2Vucy1DQSBkZXIgVGVsZW1hdGlraW5mcmFzdHJ1a3R1cjEfMB0GA1UEAwwWR0VNLlNNQ0ItQ0E5IFRFU1QtT05MWTAeFw0yMDAxMjcwMDAwMDBaFw0yNDEyMTEyMzU5NTlaMIHVMQswCQYDVQQGEwJERTERMA8GA1UEBwwIVMO2bm5pbmcxDjAMBgNVBBEMBTI1ODMyMRMwEQYDVQQJDApBbSBNYXJrdCAxMS0wKwYDVQQKDCRQcmF4aXMgTGlsbyBHcsOkZmluIGRlIEJvZXJOT1QtVkFMSUQxDTALBgNVBAQMBEJvZXIxDTALBgNVBCoMBExpbG8xEjAQBgNVBAwMCVByb2YuIERyLjEtMCsGA1UEAwwkUHJheGlzIExpbG8gR3LDpGZpbiBkZSBCb2VyVEVTVC1PTkxZMFowFAYHKoZIzj0CAQYJKyQDAwIIAQEHA0IABE+qn0H8KYaa4IszxE3FLWH9/V58z2iYu7hUVfe7PBOQKNpBw+c6wO710QhZFLr35Ks9GQGN2IBtpITcoWsR7ZKjggEZMIIBFTAdBgNVHQ4EFgQU48MFLtrb7L8kGQ1BZpNrDgGpDUEwDgYDVR0PAQH/BAQDAgMIMCAGA1UdIAQZMBcwCgYIKoIUAEwEgSMwCQYHKoIUAEwETDAfBgNVHSMEGDAWgBRiiJrE3vyj85M5y5+Q5xOaPYnMdTA4BggrBgEFBQcBAQQsMCowKAYIKwYBBQUHMAGGHGh0dHA6Ly9laGNhLmdlbWF0aWsuZGUvb2NzcC8wDAYDVR0TAQH/BAIwADBZBgUrJAgDAwRQME4wTDBKMEgwRjAWDBRCZXRyaWVic3N0w6R0dGUgQXJ6dDAJBgcqghQATAQyEyExLVNNQy1CLVRlc3RrYXJ0ZS04ODMxMTAwMDAxMTcwMzUwCgYIKoZIzj0EAwIDRwAwRAIge+TDJbeTVwj3bV78Vl9ycVZx5FhxkQYVBl6JoOwo6/wCID54Dvjk0aFAMutqYdKce00bQCaGRRzzm9Ck0dsqGfaK"
        val pkiClient =
            PKIClient {
                loader = { ListOfTrustedServiceLists(pu = ListOfTrustedServiceLists.loadFromServer(HttpClient(CIO), TrustEnvironment.TU)) }
            }
        val certDER = CertificateDataDER(cert64)
        val ocspResponse = pkiClient.ocsp(certDER)
        ocspResponse.status shouldBe OCSPResponseCertificateStatus.GOOD
    }
})
