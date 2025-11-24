package de.gematik.ti.directory.admin

import io.kotest.assertions.fail
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FeatureSpec
import io.kotest.matchers.shouldBe
import mu.KotlinLogging

class TestCreate :
    FeatureSpec({
        val logger = KotlinLogging.logger {}
        var client: Client? = null

        beforeSpec {
            client = createClient()
            client
                ?.readDirectoryEntry(mapOf("domainID" to TestCreate::class.qualifiedName!!))
                ?.forEachIndexed { index, entry ->
                    if (index > 1) {
                        fail("Etwas ist schiefgelaufen. Es wurden zu viele entries gefunden")
                    }
                    logger.info { "Deleting $entry" }
                    client?.deleteDirectoryEntry(entry.directoryEntryBase.dn?.uid!!)
                }
        }

        feature("Erstellen eines neuen Eintrag") {
            scenario("Einfaches create mit nur mit Basisdaten") {
                val entry = CreateDirectoryEntry()
                entry.directoryEntryBase = BaseDirectoryEntry("1-1" + TestCreate::class.qualifiedName!!, entryType = listOf("1"))
                entry.directoryEntryBase?.domainID = listOf(TestCreate::class.qualifiedName!!)
                entry.directoryEntryBase?.displayName = "Test vzd-cli"
                entry.directoryEntryBase?.postalCode = "12345"
                entry.directoryEntryBase?.specialization = listOf("urn:as:1.2.276.0.76.5.514:141002")
                entry.directoryEntryBase?.countryCode = "DE"
                val dn = client?.addDirectoryEntry(entry)

                val reloaded = client?.readDirectoryEntry(mapOf("uid" to dn!!.uid))?.first()

                reloaded?.directoryEntryBase?.telematikID shouldBe "1-1" + TestCreate::class.qualifiedName!!
            }
            scenario("Eintrag mit existierenden TelematikID wird abgelehnt") {
                val entry = CreateDirectoryEntry()
                entry.directoryEntryBase = BaseDirectoryEntry("1-1" + TestCreate::class.qualifiedName!!, entryType = listOf("1"))
                entry.directoryEntryBase?.domainID = listOf(TestCreate::class.qualifiedName!!)
                shouldThrow<AdminResponseException> {
                    client?.addDirectoryEntry(entry)
                }
            }
            /*
            scenario("Eintrag erzeugen und gleichzeitig Zertifikat setzten") {
                val certData =
                    CertificateDataDER(TestData.cert1)
                val entry = CreateDirectoryEntry()
                entry.directoryEntryBase =
                    BaseDirectoryEntry(certData.certificateInfo.admissionStatement.registrationNumber)
                entry.directoryEntryBase?.domainID = listOf(TestCreate::class.qualifiedName!!)
                entry.directoryEntryBase?.displayName = "Test vzd-cli"
                entry.directoryEntryBase?.cn = "Test vzd-cli"
                entry.directoryEntryBase?.streetAddress = "Teststraße 1"
                entry.directoryEntryBase?.localityName = "Teststadt"
                entry.directoryEntryBase?.postalCode = "12345"
                entry.directoryEntryBase?.specialization = listOf("urn:psc:1.3.6.1.4.1.19376.3.276.1.5.4:ALLG")
                entry.userCertificates = listOf(UserCertificate(userCertificate = certData))
                val dn = client?.addDirectoryEntry(entry)

                val reloaded = client?.readDirectoryEntry(mapOf("uid" to dn!!.uid))?.first()

                reloaded?.directoryEntryBase?.telematikID shouldBe TestData.telematikID
                reloaded?.userCertificates?.size shouldBe 1
                reloaded
                    ?.userCertificates
                    ?.first()
                    ?.userCertificate
                    ?.base64String shouldBe certData.base64String
            }
            scenario("Eintrag erzeugen und nachträglich Zertifikat setzten") {
                val certData =
                    CertificateDataDER(TestData.cert2)
                val entry = CreateDirectoryEntry()
                client
                    ?.readDirectoryEntry(mapOf("telematikID" to certData.certificateInfo.admissionStatement.registrationNumber))
                    ?.first()
                    ?.let {
                        logger.info { "Deleting $it" }
                        client?.deleteDirectoryEntry(it.directoryEntryBase.dn?.uid!!)
                    }
                entry.directoryEntryBase =
                    BaseDirectoryEntry(certData.certificateInfo.admissionStatement.registrationNumber, entryType = listOf("3"))
                entry.directoryEntryBase?.domainID = listOf(TestCreate::class.qualifiedName!!)
                val dn = client?.addDirectoryEntry(entry)

                val reloaded = client?.readDirectoryEntry(mapOf("uid" to dn!!.uid))?.first()

                reloaded?.directoryEntryBase?.telematikID shouldBe TestData.telematikID
                reloaded?.directoryEntryBase?.cn shouldBe "-"
                reloaded?.userCertificates shouldBe null

                client?.addDirectoryEntryCertificate(dn?.uid!!, UserCertificate(userCertificate = certData))

                val reloaded2 = client?.readDirectoryEntry(mapOf("uid" to dn!!.uid))?.first()
                logger.info { reloaded2 }

                reloaded2?.directoryEntryBase?.telematikID shouldBe TestData.telematikID
                reloaded2?.userCertificates?.size shouldBe 1
                reloaded2
                    ?.userCertificates
                    ?.first()
                    ?.userCertificate
                    ?.base64String shouldBe certData.base64String
            }

             */
        }

        afterSpec {
            client
                ?.readDirectoryEntry(mapOf("domainID" to TestCreate::class.qualifiedName!!))
                ?.forEachIndexed { index, entry ->
                    if (index > 1) {
                        fail("Etwas ist schiefgelaufen. Es wurden zu viele entries gefunden")
                    }
                    logger.info { "Deleting $entry" }
                    client?.deleteDirectoryEntry(entry.directoryEntryBase.dn!!.uid)
                }
        }
    })
