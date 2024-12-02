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
