/*
 * Copyright (Change Date see Readme), gematik GmbH
 *
 * Licensed under the EUPL, Version 1.2 or - as soon they will be approved by the
 * European Commission – subsequent versions of the EUPL (the "Licence").
 * You may not use this work except in compliance with the Licence.
 *
 * You find a copy of the Licence in the "Licence" file or at
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either expressed or implied.
 * In case of changes by gematik find details in the "Readme" file.
 *
 * See the Licence for the specific language governing permissions and limitations under the Licence.
 *
 * *******
 *
 * For additional notes and disclaimer from gematik and in case of changes by gematik,
 * find details in the "Readme" file.
 */

package de.gematik.ti.directory.admin

import io.kotest.core.spec.style.FeatureSpec
import io.kotest.matchers.shouldBe
import mu.KotlinLogging

private class TestState(
    val client: Client,
    val telematikID: String,
    val uid: String,
)

class TestModifyBaseAttr :
    FeatureSpec({
        val logger = KotlinLogging.logger {}
        var state: TestState? = null

        beforeSpec {
            val telematikID = "1-" + TestModifyBaseAttr::class.qualifiedName!!
            var client = createClient()

            client
                .readDirectoryEntry(mapOf("telematikID" to telematikID))
                ?.forEach { entry ->
                    logger.info { "Deleting $entry" }
                    client.deleteDirectoryEntry(entry.directoryEntryBase.dn?.uid!!)
                }
            val entry = CreateDirectoryEntry()
            entry.directoryEntryBase = BaseDirectoryEntry(telematikID, entryType = listOf("1"))
            entry.directoryEntryBase?.domainID = listOf(TestModifyBaseAttr::class.qualifiedName!!)
            entry.directoryEntryBase?.displayName = "Test vzd-cli"
            entry.directoryEntryBase?.postalCode = "12345"
            entry.directoryEntryBase?.specialization = listOf("urn:as:1.2.276.0.76.5.514:141002")
            entry.directoryEntryBase?.countryCode = "DE"
            val uid = client.addDirectoryEntry(entry).uid
            state = TestState(client, telematikID, uid)
        }

        afterSpec {
            state?.apply {
                client
                    .readDirectoryEntry(mapOf("telematikID" to telematikID))
                    ?.forEach { entry ->
                        logger.info { "Deleting $entry" }
                        client.deleteDirectoryEntry(entry.directoryEntryBase.dn!!.uid)
                    }
            }
        }

        feature("admin modify-base-attr") {
            scenario("Befehl 'admin modify-base-attr' kann strings und integers ändern") {
                val displayName = System.currentTimeMillis().toString()
                state?.apply {
                    runCLI(
                        listOf(
                            "admin",
                            "tu",
                            "modify-base-attr",
                            "-t",
                            telematikID,
                            "-s",
                            "displayName=$displayName",
                            "-s",
                            "maxKOMLEadr=7",
                        ),
                    )
                    val modifiedEntry = client.readDirectoryEntry(mapOf("uid" to uid))?.first()
                    modifiedEntry?.directoryEntryBase?.displayName shouldBe displayName
                    modifiedEntry?.directoryEntryBase?.maxKOMLEadr shouldBe 7
                }
            }

            scenario("Befehl 'admin modify-base-attr' kann eintrag deaktivieren") {
                val displayName = System.currentTimeMillis().toString() + "active"
                state?.apply {
                    runCLI(
                        listOf(
                            "admin",
                            "tu",
                            "modify-base-attr",
                            "-t",
                            telematikID,
                            "-s",
                            "displayName=$displayName",
                            "-s",
                            "active=false",
                        ),
                    )
                    val modifiedEntry = client.readDirectoryEntry(mapOf("uid" to uid))?.first()
                    modifiedEntry?.directoryEntryBase?.active shouldBe false
                    modifiedEntry?.directoryEntryBase?.displayName shouldBe displayName
                }
            }
        }
    })
