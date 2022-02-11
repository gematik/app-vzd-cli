package vzd.tools.teststuite


import io.github.cdimascio.dotenv.dotenv
import io.ktor.client.plugins.auth.providers.*
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import vzd.tools.directoryadministration.BaseDirectoryEntry
import vzd.tools.directoryadministration.Client
import vzd.tools.directoryadministration.CreateDirectoryEntry
import vzd.tools.directoryadministration.UpdateBaseDirectoryEntry
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

private val logger = KotlinLogging.logger {}

class TestClient {
    var client: Client? = null
    var dotenv = dotenv { ignoreIfMissing = true }


    @BeforeTest fun setUp() {
        this.client = Client {
            apiURL = dotenv["ADMIN_API_URL"]
            loadTokens = { BearerTokens(dotenv["ADMIN_ACCESS_TOKEN"], "") }
        }

        val entries = runBlocking { client?.readDirectoryEntry(mapOf("domainID" to TestClient::class.qualifiedName!!) ) }
        entries?.forEach {
            if (it.directoryEntryBase.telematikID.startsWith("vzd-cli")) {
                logger.debug { "Deleting ${it.directoryEntryBase}" }
                runBlocking { client?.deleteDirectoryEntry(it.directoryEntryBase.dn!!.uid) }
            }
        }
    }

    @Test fun testCreateWithOnlyTelematikID() {

        val entries = runBlocking { client?.readDirectoryEntry(mapOf("telematikID" to "vzd-cli-only-telematikID") ) }
        entries?.forEach {
            runBlocking { client?.deleteDirectoryEntry(it.directoryEntryBase.dn!!.uid) }
            return
        }

        val baseDirectoryEntry = BaseDirectoryEntry(telematikID = "vzd-cli-only-telematikID")
        val directoryEntry = CreateDirectoryEntry(baseDirectoryEntry)
        val dn = runBlocking { client?.addDirectoryEntry(directoryEntry) }
        assertNotNull(dn)

        val loadedDirectoryEntry = runBlocking { client?.readDirectoryEntry(mapOf("telematikID" to "vzd-cli-only-telematikID")) }
        assertEquals(1, loadedDirectoryEntry?.size)
        assertEquals(dn.uid, loadedDirectoryEntry?.first()?.directoryEntryBase?.dn?.uid)

    }

    @Test fun testCreateDirectoryEntry() {

        val baseDirectoryEntry = BaseDirectoryEntry(telematikID = "vzd-cli-123456890")
        baseDirectoryEntry.domainID = listOf("gematik_test", TestClient::class.qualifiedName!!)
        baseDirectoryEntry.displayName = "Uniklinik Entenhausen"
        baseDirectoryEntry.organization = "Comics Krankenhaus"
        baseDirectoryEntry.countryCode = "DE"
        baseDirectoryEntry.localityName = "Entenhausen"
        baseDirectoryEntry.stateOrProvinceName = "Bayern"
        baseDirectoryEntry.postalCode = "12345"
        baseDirectoryEntry.streetAddress = "Kaiserstraße 1"
        baseDirectoryEntry.otherName = "UK Entenhausen"
        baseDirectoryEntry.holder = listOf("gematik_test")
        val directoryEntry = CreateDirectoryEntry(baseDirectoryEntry)

        /*
        directoryEntry.userCertificates = listOf(
            UserCertificate(telematikID = "3-SMCB-Testkarte-883110000092575", userCertificate = CertificateDataDER("MIIE1jCCA76gAwIBAgIHAL3sqJ0W4zANBgkqhkiG9w0BAQsFADCBmTELMAkGA1UEBhMCREUxHzAdBgNVBAoMFmdlbWF0aWsgR21iSCBOT1QtVkFMSUQxSDBGBgNVBAsMP0luc3RpdHV0aW9uIGRlcyBHZXN1bmRoZWl0c3dlc2Vucy1DQSBkZXIgVGVsZW1hdGlraW5mcmFzdHJ1a3R1cjEfMB0GA1UEAwwWR0VNLlNNQ0ItQ0E4IFRFU1QtT05MWTAeFw0xNzEyMDQwMDAwMDBaFw0yMjEyMDMyMzU5NTlaMIGMMSowKAYDVQQDDCFBcG90aGVrZSBhbSBTcG9ydHplbnRydW1URVNULU9OTFkxHTAbBgNVBAUTFDgwMjc2ODgzMTEwMDAwMDkyNTc1MTIwMAYDVQQKDCkzLVNNQ0ItVGVzdGthcnRlLTg4MzExMDAwMDA5MjU3NU5PVC1WQUxJRDELMAkGA1UEBhMCREUwggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAwggEKAoIBAQDU2ULu8UnU5TKoDAaIvk0QugcD99XYnmnxoP5vMOf+OiVUXAgSF40ADDQm1+qj6Gf4DzRhhv73Y6sYFnFJMkin3jn7+r9wdLE9V4GPSQPD0cRSGfJgxE65+/IdSbXAvolQ/FhNStILVrgjIwpoCMzbik0nm/qxUFn8N+3LZUrpke9Jy2YDHgpUGJloY2WhovO0kNcYFHg2NhEr3rJ6Kyt72d4g8yJFMUg27R1yPf7krgm+T3+WKs4yTh1TIy9cEE6tfoXF7/TbX+bu4/HrekzqiRlpqf5A2p9HaVXa6I5aAQlYnSGNYWRu38R96q7/AL3aPmgjAxMH03z9LqQLQA7dAgMBAAGjggEsMIIBKDAdBgNVHQ4EFgQUM714rjjis5bdYH+YzmY1hH5dYdUwDAYDVR0TAQH/BAIwADBZBgUrJAgDAwRQME4wTDBKMEgwRjAXDBXDlmZmZW50bGljaGUgQXBvdGhla2UwCQYHKoIUAEwENhMgMy1TTUNCLVRlc3RrYXJ0ZS04ODMxMTAwMDAwOTI1NzUwHwYDVR0jBBgwFoAUoxJ0BoyYoR1biAcpfal1E/yGxTYwIAYDVR0gBBkwFzAKBggqghQATASBIzAJBgcqghQATARMMA4GA1UdDwEB/wQEAwIEMDBLBggrBgEFBQcBAQQ/MD0wOwYIKwYBBQUHMAGGL2h0dHA6Ly9vY3NwLnBraS50ZWxlbWF0aWstdGVzdDo4MDgwL0NNT0NTUC9PQ1NQMA0GCSqGSIb3DQEBCwUAA4IBAQAZfuzCa9T3m9oT6GGUIY3aAFBnVKu1qLrn6UzxBaXiSpJIViA6LznsFyVOF9PtZcfvHKjYxW2Iqid9lBkndqnl6Zr1D8eltfK8EYi7VFkuM2FFKNNnvBg9Hs6SsdrUK0OBw1c0HqvbwPIz5t2oVXYB/Ad70iw0CTMGFnQOnkw2DvTHQsX/JnPkfAhHMWlzMi6atMV27Fe0eAD/OE62njVEDwXScViu29O0NI8l7rzf2Xc6VsNkepz97NTKk0onUljmDUCDxqcfr+rYLXMQVQKatX/l2fv06oHdJXSvu2SdgM/J/1GtSJROaMOjO9mm4KLiopHPbLpm8OVsR52ZxBDq"))
        )
        */

        val dn = runBlocking { client?.addDirectoryEntry(directoryEntry) }
        assertNotNull(dn)
        logger.info { "Created directory entry: ${dn}" }

        val updateDirectoryEntry = UpdateBaseDirectoryEntry(
            //telematikID = "vzd-cli-123456890",
            displayName = "Uniklinik Entenhausen (modified)",
            domainID = directoryEntry.directoryEntryBase!!.domainID,
            postalCode = "54321",
            holder = listOf("gematik_test")
        )

        runBlocking { client?.modifyDirectoryEntry(dn.uid, updateDirectoryEntry) }

    }

}