package vzd.teststuite.admin

import org.bouncycastle.asn1.x500.style.BCStyle
import org.bouncycastle.jce.PrincipalUtil
import org.bouncycastle.util.encoders.Base64
import vzd.admin.cli.escape
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import kotlin.test.Test
import kotlin.test.assertEquals

class TestCert {
    @Test fun testCertificateParsingDER() {
        val certData = "MIIEFDCCA7qgAwIBAgICAiUwCgYIKoZIzj0EAwIwgbgxCzAJBgNVBAYTAkRFMTwwOgYDVQQKDDNEZXV0c2NoZSBUZWxla29tIFNlY3VyaXR5IEdtYkggLSBHMiBMb3MgMyBOT1QtVkFMSUQxSDBGBgNVBAsMP0luc3RpdHV0aW9uIGRlcyBHZXN1bmRoZWl0c3dlc2Vucy1DQSBkZXIgVGVsZW1hdGlraW5mcmFzdHJ1a3R1cjEhMB8GA1UEAwwYVFNZU0kuU01DQi1DQTQgVEVTVC1PTkxZMB4XDTIxMDgwNjAwMDAwMFoXDTI2MDgwNTIzNTk1OVowUzELMAkGA1UEBhMCREUxHjAcBgNVBAoMFTcyMjEwNzI5NjY3IE5PVC1WQUxJRDEkMCIGA1UEAwwbVGVzdHByYXhpcyBVUyA2NjcgVEVTVC1PTkxZMFowFAYHKoZIzj0CAQYJKyQDAwIIAQEHA0IABAqMmwFbYGkRflbzSwZs2pLvm1IHrcipazBr0VoiZJ8KlOv4nMdwjU7KdEI9kf/DS76mOMeZ27kQa7t73TIQ7CejggIVMIICETAfBgNVHSMEGDAWgBQzr6SrYZv1YujiKazWhuuvslV7bTAdBgNVHQ4EFgQUAv5AzXnb0QckAFHKRgh0JYaUM9wwDgYDVR0PAQH/BAQDAgMIMCAGA1UdIAQZMBcwCgYIKoIUAEwEgSMwCQYHKoIUAEwETDAMBgNVHRMBAf8EAjAAMIIBAAYDVR0fBIH4MIH1MIHyoIHvoIHshoHpbGRhcDovL2xkYXAuaGJhLnRlc3QudGVsZXNlYy5kZS9DTj1UU1lTSS5TTUNCLUNBNCUyMFRFU1QtT05MWSxPVT1JbnN0aXR1dGlvbiUyMGRlcyUyMEdlc3VuZGhlaXRzd2VzZW5zLUNBJTIwZGVyJTIwVGVsZW1hdGlraW5mcmFzdHJ1a3R1cixPPVQtU3lzdGVtcyUyMEludGVybmF0aW9uYWwlMjBHbWJIJTIwLSUyMEcyJTIwTG9zJTIwMyUyME5PVC1WQUxJRCxDPURFP0NlcnRpZmljYXRlUmV2b2NhdGlvbkxpc3QwRwYFKyQIAwMEPjA8MDowODA2MDQwFgwUQmV0cmllYnNzdMOkdHRlIEFyenQwCQYHKoIUAEwEMhMPMS0yMDcyMjEwNzI5NjY3MEIGCCsGAQUFBwEBBDYwNDAyBggrBgEFBQcwAYYmaHR0cDovL29jc3Auc21jYi50ZXN0LnRlbGVzZWMuZGUvb2NzcHIwCgYIKoZIzj0EAwIDSAAwRQIhAKiXAStZzq+rQnCzYYdAFkNWooHx7dI6dtDQXjQrD68BAiBTg0tAa7ziCe+jFjuRjOgQswfREOThAB2mrOMfHRTvCQ=="
        val bytes = Base64.decode(certData)

        val cf = CertificateFactory.getInstance("X.509")
        val cert: X509Certificate = cf.generateCertificate(bytes.inputStream()) as X509Certificate

        assertEquals("CN=Testpraxis US 667 TEST-ONLY, O=72210729667 NOT-VALID, C=DE", cert.subjectDN.name)

        val reencodedCertData = Base64.toBase64String(cert.encoded)

        assertEquals(certData, reencodedCertData)

    }

    @Test fun testCertificateParsingPEM() {
        val pem = """
         -----BEGIN CERTIFICATE-----
         MIID7TCCA5OgAwIBAgIHAiqfhujcgDAKBggqhkjOPQQDAjCBiTELMAkGA1UEBhMC
         REUxHzAdBgNVBAoMFmdlbWF0aWsgR21iSCBOT1QtVkFMSUQxODA2BgNVBAsML0hl
         aWxiZXJ1ZnNhdXN3ZWlzLUNBIGRlciBUZWxlbWF0aWtpbmZyYXN0cnVrdHVyMR8w
         HQYDVQQDDBZHRU0uSEJBLUNBMTMgVEVTVC1PTkxZMB4XDTE3MDkyODIyMDAwMFoX
         DTIyMDkyODIxNTk1OVowYDELMAkGA1UEBhMCREUxUTAKBgNVBAQMA0ZpdDAPBgNV
         BCoMCEhlaW5yaWNoMBMGA1UEBRMMMTIzNDU2Nzg5MDEzMB0GA1UEAwwWSGVpbnJp
         Y2ggRml0IFRFU1QtT05MWTBaMBQGByqGSM49AgEGCSskAwMCCAEBBwNCAARbAepF
         4IxA1yxT+aXVEmy/+Xi/x1EAvJDPPyX1hk3gSHMVp1U8t6daNzyapnnUPUrheMTv
         5twe2ttS8mSqzv/Ro4ICCzCCAgcwHQYDVR0OBBYEFM2IsEJxnCekoqV3f6dykh6R
         oY1CMAwGA1UdEwEB/wQCMAAwHAYDVR0RBBUwE4ERdm9uc2lubmVuQHRlc3QuZGUw
         gckGBSskCAMDBIG/MIG8pDAwLjELMAkGA1UEBhMCREUxHzAdBgNVBAoMFlB0SyBT
         Y2hsZXN3aWctSG9sc3RlaW4wgYcwgYQwgYEwQzAnDCVQc3ljaG9sb2dpc2NoZS8t
         ciBQc3ljaG90aGVyYXBldXQvLWluMAkGByqCFABMBC4TDTQtMTFhNDEwMy0wMDEw
         OjAtDCtLaW5kZXItIHVuZCBKdWdlbmRsaWNoZW5wc3ljaG90aGVyYXBldXQvLWlu
         MAkGByqCFABMBC8wHwYDVR0jBBgwFoAUCvsM1dSTVD1lgD56IAKVNJaAG8AwcAYD
         VR0gBGkwZzAJBgcqghQATARKMFoGCCqCFABMBIERME4wTAYIKwYBBQUHAgEWQGh0
         dHA6Ly93d3cuZS1wc3ljaG90aGVyYXBldXRlbmF1c3dlaXMuZGUvcG9saWNpZXMv
         RUVfcG9saWN5Lmh0bWwwDgYDVR0PAQH/BAQDAgQwMEsGCCsGAQUFBwEBBD8wPTA7
         BggrBgEFBQcwAYYvaHR0cDovL29jc3AucGtpLnRlbGVtYXRpay10ZXN0OjgwODAv
         Q01PQ1NQL09DU1AwCgYIKoZIzj0EAwIDSAAwRQIhAIpJ+mE1DtJhN3HuCe7Gh236
         ua1GubZNlYBsuNnR1QB/AiBmcwD1CVoi3g67dvPtHLtyz/Osq7Tb49Odo5WnMz5J
         pQ==
         -----END CERTIFICATE-----
        """.trimIndent()

        val cf = CertificateFactory.getInstance("X.509")
        val cert: X509Certificate = cf.generateCertificate(pem.byteInputStream()) as X509Certificate

        val subject = PrincipalUtil.getSubjectX509Principal(cert)

        assertEquals("Fit", subject.getValues(BCStyle.SURNAME).firstElement())

    }

    @Test fun escapeString() {
        val str = "9-243423434\n";

        assertEquals("9-243423434\\n", str.escape())
    }

}
