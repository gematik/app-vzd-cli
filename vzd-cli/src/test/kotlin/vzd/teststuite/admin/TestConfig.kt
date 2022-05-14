package vzd.teststuite.admin

import io.kotest.core.spec.style.FeatureSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import vzd.admin.client.DefaultConfig
import vzd.admin.client.FileConfigProvider
import kotlin.io.path.Path
import kotlin.io.path.createTempDirectory
import kotlin.io.path.deleteIfExists

class TestConfig : FeatureSpec({
    val configDir = createTempDirectory()
    val configPath = Path(configDir.toString(), "directory-admin-test.yaml")

    beforeSpec {
        configPath.deleteIfExists()
    }

    feature("Neuen Config erzeugen") {
        scenario("Default config hat 3 Umgebungen") {
            val config = DefaultConfig
            config.environments.size shouldBe 3
            config.environments.keys shouldContain "tu"
        }
        scenario("Beim resetten der Konfiguration wird die Konfig-Datei neu erzeugt") {
            configPath.toFile().exists() shouldBe false
            FileConfigProvider(configPath).reset()
            configPath.toFile().exists() shouldBe true
        }
    }

    afterSpec {
    }
})
