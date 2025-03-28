package de.gematik.ti.directory.admin

import de.gematik.ti.directory.cli.util.KeyStoreVaultProvider
import io.kotest.core.spec.style.FeatureSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlin.io.path.Path
import kotlin.io.path.createTempDirectory
import kotlin.io.path.deleteIfExists

class TestVault :
    FeatureSpec({
        val testServiceName = "directory-vault-test"
        val vaultDir = createTempDirectory()
        val vaultPath = Path(vaultDir.toString(), "directory-vault-test.keystore")
        val badPassword = "BadPassword"
        val longSecret =
            """
            Lorem ipsum dolor sit amet, consectetur adipiscing elit. Mauris eget mattis ipsum. 
            Phasellus eleifend feugiat nisl vitae pellentesque. Sed viverra a ex et sagittis.
            """.trimIndent()

        beforeSpec {
            vaultPath.deleteIfExists()
        }

        feature("Secrets verwalten") {
            scenario("Credentials KeyStore wird automatisch erzeugt") {
                vaultPath.toFile().exists() shouldBe false
                val vault = KeyStoreVaultProvider(vaultPath).open(badPassword, testServiceName)
                vault.store("ru", "test1", longSecret)
                vaultPath.toFile().exists() shouldBe true
                val vault2 = KeyStoreVaultProvider(vaultPath).open(badPassword, testServiceName)
                vault2.get("ru")?.secret shouldBe longSecret
            }
            scenario("Löschen von existierenden Secret") {
                val vault = KeyStoreVaultProvider(vaultPath).open(badPassword, testServiceName)
                vault.store("ru", "test-to-be-deleted", "secret")
                val vault2 = KeyStoreVaultProvider(vaultPath).open(badPassword, testServiceName)
                vault2.get("ru") shouldNotBe null
                vault2.delete("ru")
                vault2.get("ru") shouldBe null
                val vault3 = KeyStoreVaultProvider(vaultPath).open(badPassword, testServiceName)
                vault3.get("ru") shouldBe null
            }
            scenario("Credentials leeren") {
                val vault = KeyStoreVaultProvider(vaultPath).open(badPassword, testServiceName)
                vault.store("ru", "id1", "secret1")
                vault.store("tu", "id2", "secret2")
                vault.store("pu", "id2", "secret3")
                vault.get("ru") shouldNotBe null
                vault.get("tu") shouldNotBe null
                vault.get("pu") shouldNotBe null
                KeyStoreVaultProvider(vaultPath).open(badPassword, testServiceName).clear()
                val vault2 = KeyStoreVaultProvider(vaultPath).open(badPassword, testServiceName)
                vault2.get("ru") shouldBe null
                vault2.get("tu") shouldBe null
                vault2.get("pu") shouldBe null
            }
            scenario("Credentials zurücksetzen") {
                val vault = KeyStoreVaultProvider(vaultPath).open(badPassword, testServiceName)
                vault.store("ru", "id1", "secret1")
                vault.store("tu", "id2", "secret2")
                vault.store("pu", "id2", "secret3")
                vault.get("ru") shouldNotBe null
                vault.get("tu") shouldNotBe null
                vault.get("pu") shouldNotBe null
                KeyStoreVaultProvider(vaultPath).purge()
                val vault2 = KeyStoreVaultProvider(vaultPath).open(badPassword, testServiceName)
                vault2.get("ru") shouldBe null
                vault2.get("tu") shouldBe null
                vault2.get("pu") shouldBe null
            }
            scenario("Regex") {
                val serviceName = "urn:gematik:directory:admin"
                val regex = "^$serviceName:([\\w\\p{L}\\-_]+):([\\w\\p{L}\\-_]+)".toRegex()
                val s = "urn:gematik:directory:admin:rü:test_1-a-üäß"
                regex.matches(s) shouldBe true
                regex
                    .matchEntire(s)
                    ?.groups
                    ?.get(1)
                    ?.value shouldBe "rü"
                regex
                    .matchEntire(s)
                    ?.groups
                    ?.get(2)
                    ?.value shouldBe "test_1-a-üäß"
            }
        }

        afterSpec {
        }
    })
