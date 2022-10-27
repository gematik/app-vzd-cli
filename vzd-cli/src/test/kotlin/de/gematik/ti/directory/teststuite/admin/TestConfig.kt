package de.gematik.ti.directory.teststuite.admin

import de.gematik.ti.directory.admin.FileConfigStore
import io.kotest.core.spec.style.FeatureSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
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
            val config = FileConfigStore(configPath).value
            config.environments.size shouldBe 3
            config.environments.keys shouldContain "tu"
            configPath.deleteIfExists()
        }
        scenario("Beim resetten der Konfiguration wird die Konfig-Datei neu erzeugt") {
            configPath.toFile().exists() shouldBe false
            FileConfigStore(configPath).reset()
            configPath.toFile().exists() shouldBe true
            configPath.deleteIfExists()
        }
    }

    afterSpec {
    }
})
