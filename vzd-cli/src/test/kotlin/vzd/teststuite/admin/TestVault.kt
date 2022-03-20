package vzd.teststuite.admin

import io.kotest.core.spec.style.FeatureSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import mu.KotlinLogging
import vzd.admin.client.KeyStoreVaultProvider
import java.io.File
import kotlin.io.path.Path
import kotlin.io.path.createTempDirectory
import kotlin.io.path.deleteIfExists

class TestVault : FeatureSpec({
    val logger = KotlinLogging.logger {}
    val vaultDir = createTempDirectory()
    val vaultPath = Path(vaultDir.toString(), "directory-vault-test.keystore")
    val badPassword = "BadPassword"
    val longSecret = """
        Lorem ipsum dolor sit amet, consectetur adipiscing elit. Mauris eget mattis ipsum. 
        Phasellus eleifend feugiat nisl vitae pellentesque. Sed viverra a ex et sagittis.
    """.trimIndent()
    
    beforeSpec {
        vaultPath.deleteIfExists()
    }

    feature("Secrets verwalten") {
        scenario("Credentials KeyStore wird automatisch erzeugt") {
            vaultPath.toFile().exists() shouldBe false
            val provider = KeyStoreVaultProvider(badPassword, vaultPath)
            provider.store("ru", "test1", longSecret)
            vaultPath.toFile().exists() shouldBe true
            val provider2 = KeyStoreVaultProvider(badPassword, vaultPath)
            provider2.get("ru")?.secret shouldBe longSecret
        }
        scenario("Löschen von existierenden Secret") {
            val provider = KeyStoreVaultProvider(badPassword, vaultPath)
            provider.store("ru", "test-to-be-deleted", "secret")
            val provider2 = KeyStoreVaultProvider(badPassword, vaultPath)
            provider2.get("ru") shouldNotBe null
            provider2.delete("ru")
            provider2.get("ru") shouldBe null
            val provider3 = KeyStoreVaultProvider(badPassword, vaultPath)
            provider3.get("ru") shouldBe null
        }
        scenario("Credentials leeren") {
            val provider = KeyStoreVaultProvider(badPassword, vaultPath)
            provider.store("ru", "id1", "secret1")
            provider.store("tu", "id2", "secret2")
            provider.store("pu", "id2", "secret3")
            provider.get("ru") shouldNotBe null
            provider.get("tu") shouldNotBe null
            provider.get("pu") shouldNotBe null
            KeyStoreVaultProvider(badPassword, vaultPath).clear()
            val provider3 = KeyStoreVaultProvider(badPassword, vaultPath)
            provider3.get("ru") shouldBe null
            provider3.get("tu") shouldBe null
            provider3.get("pu") shouldBe null

        }
        scenario("Credentials zurücksetzen") {
            val provider = KeyStoreVaultProvider(badPassword, vaultPath)
            provider.store("ru", "id1", "secret1")
            provider.store("tu", "id2", "secret2")
            provider.store("pu", "id2", "secret3")
            provider.get("ru") shouldNotBe null
            provider.get("tu") shouldNotBe null
            provider.get("pu") shouldNotBe null
            KeyStoreVaultProvider(badPassword, vaultPath, reset = true)
            val provider3 = KeyStoreVaultProvider(badPassword, vaultPath)
            provider3.get("ru") shouldBe null
            provider3.get("tu") shouldBe null
            provider3.get("pu") shouldBe null

        }
        scenario("Regex") {
            val SERVICE_NAME = "urn:gematik:directory:admin"
            val REGEX = "^$SERVICE_NAME:([\\w\\p{L}\\-_]+):([\\w\\p{L}\\-_]+)".toRegex()
            val s = "urn:gematik:directory:admin:rü:test_1-a-üäß"
            REGEX.matches(s) shouldBe true
            REGEX.matchEntire(s)?.groups?.get(1)?.value shouldBe "rü"
            REGEX.matchEntire(s)?.groups?.get(2)?.value shouldBe "test_1-a-üäß"
        }
    }


    afterSpec {
    }

})