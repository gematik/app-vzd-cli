package vzd.tools.teststuite


import io.github.cdimascio.dotenv.dotenv
import io.ktor.client.plugins.auth.providers.*
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import vzd.tools.directoryadministration.*
import kotlin.test.BeforeTest
import kotlin.test.Test

private val logger = KotlinLogging.logger {}

class TestCreateDirectoryEntry {
    var client: Client? = null
    var dotenv = dotenv { ignoreIfMissing = true }

    @BeforeTest fun setUp() {
        this.client = Client {
            apiURL = dotenv["ADMIN_API_URL"]
            loadTokens = { BearerTokens(dotenv["ADMIN_ACCESS_TOKEN"], "") }
        }

        var entries = runBlocking { client?.readDirectoryEntry(mapOf("telematikID" to "3-vzd-tools-123456890") ) }
        if (entries?.size == 1) {
            runBlocking { client?.deleteDirectoryEntry(entries[0].directoryEntryBase.dn!!.uid) }
        }
    }

    @Test fun testCreateDirectoryEntry() {

        val baseDirectoryEntry = BaseDirectoryEntry(telematikID = "3-vzd-tools-123456890")
        baseDirectoryEntry.domainID = listOf("gematik_test", TestCreateDirectoryEntry::class.qualifiedName!!)
        baseDirectoryEntry.displayName = "Uniklinik Entenhausen"
        baseDirectoryEntry.organization = "Comics Krankenhaus"
        baseDirectoryEntry.countryCode = "DE";
        baseDirectoryEntry.localityName = "Entenhausen"
        baseDirectoryEntry.stateOrProvinceName = "Bayern"
        baseDirectoryEntry.postalCode = "12345"
        baseDirectoryEntry.streetAddress = "Kaiserstraße 1"
        baseDirectoryEntry.otherName = "UK Entenhausen"
        val directoryEntry = CreateDirectoryEntry(baseDirectoryEntry)

        /*
        directoryEntry.userCertificates = listOf(
            UserCertificate(telematikID = "3-SMCB-Testkarte-883110000092575", userCertificate = CertificateDataDER("MIIE1jCCA76gAwIBAgIHAL3sqJ0W4zANBgkqhkiG9w0BAQsFADCBmTELMAkGA1UEBhMCREUxHzAdBgNVBAoMFmdlbWF0aWsgR21iSCBOT1QtVkFMSUQxSDBGBgNVBAsMP0luc3RpdHV0aW9uIGRlcyBHZXN1bmRoZWl0c3dlc2Vucy1DQSBkZXIgVGVsZW1hdGlraW5mcmFzdHJ1a3R1cjEfMB0GA1UEAwwWR0VNLlNNQ0ItQ0E4IFRFU1QtT05MWTAeFw0xNzEyMDQwMDAwMDBaFw0yMjEyMDMyMzU5NTlaMIGMMSowKAYDVQQDDCFBcG90aGVrZSBhbSBTcG9ydHplbnRydW1URVNULU9OTFkxHTAbBgNVBAUTFDgwMjc2ODgzMTEwMDAwMDkyNTc1MTIwMAYDVQQKDCkzLVNNQ0ItVGVzdGthcnRlLTg4MzExMDAwMDA5MjU3NU5PVC1WQUxJRDELMAkGA1UEBhMCREUwggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAwggEKAoIBAQDU2ULu8UnU5TKoDAaIvk0QugcD99XYnmnxoP5vMOf+OiVUXAgSF40ADDQm1+qj6Gf4DzRhhv73Y6sYFnFJMkin3jn7+r9wdLE9V4GPSQPD0cRSGfJgxE65+/IdSbXAvolQ/FhNStILVrgjIwpoCMzbik0nm/qxUFn8N+3LZUrpke9Jy2YDHgpUGJloY2WhovO0kNcYFHg2NhEr3rJ6Kyt72d4g8yJFMUg27R1yPf7krgm+T3+WKs4yTh1TIy9cEE6tfoXF7/TbX+bu4/HrekzqiRlpqf5A2p9HaVXa6I5aAQlYnSGNYWRu38R96q7/AL3aPmgjAxMH03z9LqQLQA7dAgMBAAGjggEsMIIBKDAdBgNVHQ4EFgQUM714rjjis5bdYH+YzmY1hH5dYdUwDAYDVR0TAQH/BAIwADBZBgUrJAgDAwRQME4wTDBKMEgwRjAXDBXDlmZmZW50bGljaGUgQXBvdGhla2UwCQYHKoIUAEwENhMgMy1TTUNCLVRlc3RrYXJ0ZS04ODMxMTAwMDAwOTI1NzUwHwYDVR0jBBgwFoAUoxJ0BoyYoR1biAcpfal1E/yGxTYwIAYDVR0gBBkwFzAKBggqghQATASBIzAJBgcqghQATARMMA4GA1UdDwEB/wQEAwIEMDBLBggrBgEFBQcBAQQ/MD0wOwYIKwYBBQUHMAGGL2h0dHA6Ly9vY3NwLnBraS50ZWxlbWF0aWstdGVzdDo4MDgwL0NNT0NTUC9PQ1NQMA0GCSqGSIb3DQEBCwUAA4IBAQAZfuzCa9T3m9oT6GGUIY3aAFBnVKu1qLrn6UzxBaXiSpJIViA6LznsFyVOF9PtZcfvHKjYxW2Iqid9lBkndqnl6Zr1D8eltfK8EYi7VFkuM2FFKNNnvBg9Hs6SsdrUK0OBw1c0HqvbwPIz5t2oVXYB/Ad70iw0CTMGFnQOnkw2DvTHQsX/JnPkfAhHMWlzMi6atMV27Fe0eAD/OE62njVEDwXScViu29O0NI8l7rzf2Xc6VsNkepz97NTKk0onUljmDUCDxqcfr+rYLXMQVQKatX/l2fv06oHdJXSvu2SdgM/J/1GtSJROaMOjO9mm4KLiopHPbLpm8OVsR52ZxBDq"))
        )
        */

        val dn = runBlocking { client?.addDirectoryEntry(directoryEntry) }

        logger.info { "Created directory entry: ${dn}" }
    }

}