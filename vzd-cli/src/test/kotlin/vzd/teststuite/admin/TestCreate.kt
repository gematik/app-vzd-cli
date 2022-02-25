package vzd.teststuite.admin

import io.kotest.assertions.fail
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FeatureSpec
import io.kotest.matchers.shouldBe
import mu.KotlinLogging
import vzd.admin.client.*


class TestCreate : FeatureSpec({
    val logger = KotlinLogging.logger {}
    var client: Client? = null

    beforeSpec {
        client = createClient()
        client?.readDirectoryEntry(mapOf("domainID" to TestCreate::class.qualifiedName!!))
            ?.forEachIndexed { index, entry ->
                if (index > 1) {
                    fail("Etwas ist schiefgelaufen. Es wurden zu viele entries gefunden")
                }
                logger.info { "Deleting $entry" }
                client?.deleteDirectoryEntry(entry.directoryEntryBase.dn?.uid!!)
            }
    }

    feature("Erstellen eines neuen Eintrag") {
        scenario("Einfaches create mit nur TelematikID und domainID") {
            val entry = CreateDirectoryEntry()
            entry.directoryEntryBase = BaseDirectoryEntry("1-" + TestCreate::class.qualifiedName!!)
            entry.directoryEntryBase?.domainID = listOf(TestCreate::class.qualifiedName!!)
            val dn = client?.addDirectoryEntry(entry)

            val reloaded = client?.readDirectoryEntry(mapOf("uid" to dn!!.uid))?.first()

            reloaded?.directoryEntryBase?.telematikID shouldBe "1-" + TestCreate::class.qualifiedName!!

        }
        scenario("Eintrag mit existierenden TelematikID wird abgelehnt") {
            val entry = CreateDirectoryEntry()
            entry.directoryEntryBase = BaseDirectoryEntry("1-" + TestCreate::class.qualifiedName!!)
            entry.directoryEntryBase?.domainID = listOf(TestCreate::class.qualifiedName!!)
            shouldThrow<VZDResponseException> {
                client?.addDirectoryEntry(entry)
            }

        }
        scenario("Eintrag erzeugen und gleichzeitig Zertifikat setzten") {
            val certData = CertificateDataDER("MIIFHzCCBAegAwIBAgIHAO9cve8XkDANBgkqhkiG9w0BAQsFADCBiDELMAkGA1UEBhMCREUxHzAdBgNVBAoMFmdlbWF0aWsgR21iSCBOT1QtVkFMSUQxODA2BgNVBAsML0hlaWxiZXJ1ZnNhdXN3ZWlzLUNBIGRlciBUZWxlbWF0aWtpbmZyYXN0cnVrdHVyMR4wHAYDVQQDDBVHRU0uSEJBLUNBNCBURVNULU9OTFkwHhcNMTYwODE4MTEyMDAwWhcNMTgwODE4MTEyMDAwWjCBhzELMAkGA1UEBhMCREUxeDAsBgNVBAMMJURvbWluaXF1ZS1NaWNoZWxsZSBPbGRlbmJ1cmdURVNULU9OTFkwGQYDVQQqDBJEb21pbmlxdWUtTWljaGVsbGUwEAYDVQQEDAlPbGRlbmJ1cmcwGwYDVQQFExQ4MDI3Njg4MzExMDAwMDA2NjAyMDCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBAJ01K5DWL3cW2BLrrLcojshwfF7o3fw5XKTN0cngY9hNJgxnOZX3vlzlMJ5kMkp71lgiGKM/ZgYoyQHrlQgqK18mv6ESOorZCjActBUkNQ24j0KZuBj6g6YDRpwRQjBDEvPBGJLWU01QBz2N+rEkmrnbqobIabewM8IEk31dvNLGGOh5yQc06qLKv9KJLhyHOJDafcRjMvk39XEQvwVbsTrNMjU6PliV7QFoWyiaqZQJf5daMipUMs/12GbUFX4tOaFb99jfrLBOzKT+I2xVEnOJkN/C3+BsGHPhhRW5yinWwvGcFiuV9Bv34cGr+aRv+ujOHiqumCrtuHfat5bbmvsCAwEAAaOCAYswggGHMB0GA1UdDgQWBBQzsK6xvsz9IZcIMUO/xM36v2MG5jAMBgNVHRMBAf8EAjAAMGcGBSskCAMDBF4wXKQkMCIxCzAJBgNVBAYTAkRFMRMwEQYDVQQKDArDhEsgQmVybGluMDQwMjAwMC4wDgwMw4RyenRpbi9Bcnp0MAkGByqCFABMBB4TETEtMjEyMzQ1Njdhc2RmZ2hiMB8GA1UdIwQYMBaAFIZGhBzhBZbclql9izv4ZTSemNnmMHEGA1UdIARqMGgwCQYHKoIUAEwESjBNBggqghQATASBETBBMD8GCCsGAQUFBwIBFjNodHRwOi8vd3d3LmUtYXJ6dGF1c3dlaXMuZGUvcG9saWNpZXMvRUVfcG9saWN5Lmh0bWwwDAYKKwYBBAGCzTMBATAOBgNVHQ8BAf8EBAMCBDAwSwYIKwYBBQUHAQEEPzA9MDsGCCsGAQUFBzABhi9odHRwOi8vb2NzcC5wa2kudGVsZW1hdGlrLXRlc3Q6ODA4MC9DTU9DU1AvT0NTUDANBgkqhkiG9w0BAQsFAAOCAQEAJ3LNfbj8lSyfb/sBneAoTYUSvzj2ya3EYSVrmql58xiKCP56sN5mIyJLnYDc/FyS9KKGUDrBQ9su4h+tBK4+uu8ZM6ejdD3JBskYKzTVbLTjIeZKl/qiN/KAmU8D3r+llp4pkEDl8SEdDqpozO2YtoC53WocNxTHffgTy1LIaroBrU93pGpVr6Stf/8w7Tlkrkv+0U96aEI4tX521cT3o8XG29FXr5Lh/xbQMgD5HOfyBkzBYtlMeqGOYBfFSYcB0Bgtj5oIwjg++i021LIZfHvHlMT8sywE2vGMQMDGuzGdUKKjGsK23SzbBjjtkMpnb9lGc46dgZlHG3UKDpPKqg==")
            val entry = CreateDirectoryEntry()
            entry.directoryEntryBase = BaseDirectoryEntry(certData.toCertificateInfo().admissionStatement.registrationNumber)
            entry.directoryEntryBase?.domainID = listOf(TestCreate::class.qualifiedName!!)
            entry.userCertificates = listOf(UserCertificate(userCertificate = certData))
            val dn = client?.addDirectoryEntry(entry)

            val reloaded = client?.readDirectoryEntry(mapOf("uid" to dn!!.uid))?.first()

            reloaded?.directoryEntryBase?.telematikID shouldBe "1-21234567asdfghb"
            reloaded?.directoryEntryBase?.givenName shouldBe "Dominique-Michelle"
            reloaded?.userCertificates?.size shouldBe 1
            reloaded?.userCertificates?.first()?.userCertificate?.base64String shouldBe certData.base64String
        }
        scenario("Eintrag erzeugen und nachträglich Zertifikat setzten") {
            val certData = CertificateDataDER("MIIFHzCCBAegAwIBAgIHAO9cve8XkDANBgkqhkiG9w0BAQsFADCBiDELMAkGA1UEBhMCREUxHzAdBgNVBAoMFmdlbWF0aWsgR21iSCBOT1QtVkFMSUQxODA2BgNVBAsML0hlaWxiZXJ1ZnNhdXN3ZWlzLUNBIGRlciBUZWxlbWF0aWtpbmZyYXN0cnVrdHVyMR4wHAYDVQQDDBVHRU0uSEJBLUNBNCBURVNULU9OTFkwHhcNMTYwODE4MTEyMDAwWhcNMTgwODE4MTEyMDAwWjCBhzELMAkGA1UEBhMCREUxeDAsBgNVBAMMJURvbWluaXF1ZS1NaWNoZWxsZSBPbGRlbmJ1cmdURVNULU9OTFkwGQYDVQQqDBJEb21pbmlxdWUtTWljaGVsbGUwEAYDVQQEDAlPbGRlbmJ1cmcwGwYDVQQFExQ4MDI3Njg4MzExMDAwMDA2NjAyMDCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBAJ01K5DWL3cW2BLrrLcojshwfF7o3fw5XKTN0cngY9hNJgxnOZX3vlzlMJ5kMkp71lgiGKM/ZgYoyQHrlQgqK18mv6ESOorZCjActBUkNQ24j0KZuBj6g6YDRpwRQjBDEvPBGJLWU01QBz2N+rEkmrnbqobIabewM8IEk31dvNLGGOh5yQc06qLKv9KJLhyHOJDafcRjMvk39XEQvwVbsTrNMjU6PliV7QFoWyiaqZQJf5daMipUMs/12GbUFX4tOaFb99jfrLBOzKT+I2xVEnOJkN/C3+BsGHPhhRW5yinWwvGcFiuV9Bv34cGr+aRv+ujOHiqumCrtuHfat5bbmvsCAwEAAaOCAYswggGHMB0GA1UdDgQWBBQzsK6xvsz9IZcIMUO/xM36v2MG5jAMBgNVHRMBAf8EAjAAMGcGBSskCAMDBF4wXKQkMCIxCzAJBgNVBAYTAkRFMRMwEQYDVQQKDArDhEsgQmVybGluMDQwMjAwMC4wDgwMw4RyenRpbi9Bcnp0MAkGByqCFABMBB4TETEtMjEyMzQ1Njdhc2RmZ2hiMB8GA1UdIwQYMBaAFIZGhBzhBZbclql9izv4ZTSemNnmMHEGA1UdIARqMGgwCQYHKoIUAEwESjBNBggqghQATASBETBBMD8GCCsGAQUFBwIBFjNodHRwOi8vd3d3LmUtYXJ6dGF1c3dlaXMuZGUvcG9saWNpZXMvRUVfcG9saWN5Lmh0bWwwDAYKKwYBBAGCzTMBATAOBgNVHQ8BAf8EBAMCBDAwSwYIKwYBBQUHAQEEPzA9MDsGCCsGAQUFBzABhi9odHRwOi8vb2NzcC5wa2kudGVsZW1hdGlrLXRlc3Q6ODA4MC9DTU9DU1AvT0NTUDANBgkqhkiG9w0BAQsFAAOCAQEAJ3LNfbj8lSyfb/sBneAoTYUSvzj2ya3EYSVrmql58xiKCP56sN5mIyJLnYDc/FyS9KKGUDrBQ9su4h+tBK4+uu8ZM6ejdD3JBskYKzTVbLTjIeZKl/qiN/KAmU8D3r+llp4pkEDl8SEdDqpozO2YtoC53WocNxTHffgTy1LIaroBrU93pGpVr6Stf/8w7Tlkrkv+0U96aEI4tX521cT3o8XG29FXr5Lh/xbQMgD5HOfyBkzBYtlMeqGOYBfFSYcB0Bgtj5oIwjg++i021LIZfHvHlMT8sywE2vGMQMDGuzGdUKKjGsK23SzbBjjtkMpnb9lGc46dgZlHG3UKDpPKqg==")
            val entry = CreateDirectoryEntry()
            client?.readDirectoryEntry(mapOf("telematikID" to certData.toCertificateInfo().admissionStatement.registrationNumber))?.first()?.let {
                logger.info { "Deleting $it" }
                client?.deleteDirectoryEntry(it.directoryEntryBase.dn?.uid!!)
            }
            entry.directoryEntryBase = BaseDirectoryEntry(certData.toCertificateInfo().admissionStatement.registrationNumber)
            entry.directoryEntryBase?.domainID = listOf(TestCreate::class.qualifiedName!!)
            val dn = client?.addDirectoryEntry(entry)

            val reloaded = client?.readDirectoryEntry(mapOf("uid" to dn!!.uid))?.first()

            reloaded?.directoryEntryBase?.telematikID shouldBe "1-21234567asdfghb"
            reloaded?.directoryEntryBase?.givenName shouldBe null
            reloaded?.userCertificates shouldBe null

            client?.addDirectoryEntryCertificate(dn?.uid!!, UserCertificate(userCertificate = certData ))

            val reloaded2 = client?.readDirectoryEntry(mapOf("uid" to dn!!.uid))?.first()
            logger.info { reloaded2 }

            reloaded2?.directoryEntryBase?.telematikID shouldBe "1-21234567asdfghb"
            reloaded2?.directoryEntryBase?.givenName shouldBe "Dominique-Michelle"
            reloaded2?.userCertificates?.size shouldBe 1
            reloaded2?.userCertificates?.first()?.userCertificate?.base64String shouldBe certData.base64String

        }
    }

    afterSpec {
        client?.readDirectoryEntry(mapOf("domainID" to TestCreate::class.qualifiedName!!))
            ?.forEachIndexed { index, entry ->
                if (index > 1) {
                    fail("Etwas ist schiefgelaufen. Es wurden zu viele entries gefunden")
                }
                logger.info { "Deleting $entry" }
                client?.deleteDirectoryEntry(entry.directoryEntryBase.dn!!.uid)
            }

    }

})