/*
Copyright 2026, gematik GmbH

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

*******

For additional notes and disclaimer from gematik and in case of changes
by gematik, find details in the "Readme" file.
*/

package de.gematik.ti.directory.admin

import io.ktor.client.plugins.auth.providers.*
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import kotlin.test.BeforeTest
import kotlin.test.Test
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
    fun testCreateDirectoryEntry() {
        val baseDirectoryEntry = BaseDirectoryEntry(telematikID = "vzd-cli-123456890", entryType = listOf("1"))
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

        /*
        val updateDirectoryEntry =
            UpdateBaseDirectoryEntry(
                // telematikID = "vzd-cli-123456890",
                displayName = "Uniklinik Entenhausen (modified)",
                domainID = directoryEntry.directoryEntryBase!!.domainID,
                postalCode = "54321",
                holder = listOf("gematik_test"),
            )

        runBlocking { client?.modifyDirectoryEntry(dn.uid, updateDirectoryEntry) }
         */
    }
}
