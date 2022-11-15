package de.gematik.ti.directory.admin

import io.kotest.core.spec.style.FeatureSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
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
            val config = AdminConfigFileStore(configPath).value
            config.environments.size shouldBe 3
            config.environments.keys shouldContain "tu"
            configPath.deleteIfExists()
        }
        scenario("Beim resetten der Konfiguration wird die Konfig-Datei neu erzeugt") {
            configPath.toFile().exists() shouldBe false
            AdminConfigFileStore(configPath).reset()
            configPath.toFile().exists() shouldBe true
            configPath.deleteIfExists()
        }
    }

    feature("vzd-cli admin config") {
        scenario("vzd-cli admin config get") {
            cli("admin", "config", "get") {
                it shouldContain "https://vzdpflege-ref.vzd.ti-dienste.de:9543"
                it shouldContain "https://vzdpflege-test.vzd.ti-dienste.de:9543"
            }
        }
        scenario("vzd-cli admin config get environments.tu") {
            cli("admin", "config", "get", "environments.tu") {
                it shouldNotContain "https://vzdpflege-ref.vzd.ti-dienste.de:9543"
                it shouldContain "https://vzdpflege-test.vzd.ti-dienste.de:9543"
            }
        }
    }

    afterSpec {
    }
})
