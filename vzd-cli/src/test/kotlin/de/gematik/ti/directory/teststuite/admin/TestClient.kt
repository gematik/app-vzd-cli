package de.gematik.ti.directory.teststuite.admin

import de.gematik.ti.directory.admin.BaseDirectoryEntry
import de.gematik.ti.directory.admin.Client
import de.gematik.ti.directory.admin.CreateDirectoryEntry
import de.gematik.ti.directory.admin.UpdateBaseDirectoryEntry
import io.ktor.client.plugins.auth.providers.*
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

private val logger = KotlinLogging.logger {}

class TestClient {
    var client: Client? = null

    @BeforeTest
    fun setUp() {
        this.client = createClient()

        val entries = runBlocking { client?.readDirectoryEntry(mapOf("domainID" to TestClient::class.qualifiedName!!)) }
        entries?.forEach {
            if (it.directoryEntryBase.telematikID.startsWith("vzd-cli")) {
                logger.debug { "Deleting ${it.directoryEntryBase}" }
                runBlocking { client?.deleteDirectoryEntry(it.directoryEntryBase.dn!!.uid) }
            }
        }
    }

    @Test
    fun testCreateWithOnlyTelematikID() {
        val entries = runBlocking { client?.readDirectoryEntry(mapOf("telematikID" to "vzd-cli-only-telematikID")) }
        entries?.forEach {
            runBlocking { client?.deleteDirectoryEntry(it.directoryEntryBase.dn!!.uid) }
            return
        }

        val baseDirectoryEntry = BaseDirectoryEntry(telematikID = "vzd-cli-only-telematikID")
        val directoryEntry = CreateDirectoryEntry(baseDirectoryEntry)
        val dn = runBlocking { client?.addDirectoryEntry(directoryEntry) }
        assertNotNull(dn)

        val loadedDirectoryEntry =
            runBlocking { client?.readDirectoryEntry(mapOf("telematikID" to "vzd-cli-only-telematikID")) }
        assertEquals(1, loadedDirectoryEntry?.size)
        assertEquals(dn.uid, loadedDirectoryEntry?.first()?.directoryEntryBase?.dn?.uid)
    }

    @Test
    fun testCreateDirectoryEntry() {
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

        val dn = runBlocking { client?.addDirectoryEntry(directoryEntry) }
        assertNotNull(dn)
        logger.info { "Created directory entry: $dn" }

        val updateDirectoryEntry = UpdateBaseDirectoryEntry(
            // telematikID = "vzd-cli-123456890",
            displayName = "Uniklinik Entenhausen (modified)",
            domainID = directoryEntry.directoryEntryBase!!.domainID,
            postalCode = "54321",
            holder = listOf("gematik_test")
        )

        runBlocking { client?.modifyDirectoryEntry(dn.uid, updateDirectoryEntry) }
    }
}
