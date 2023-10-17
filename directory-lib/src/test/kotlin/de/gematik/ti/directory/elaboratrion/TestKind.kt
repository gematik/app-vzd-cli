package de.gematik.ti.directory.elaboratrion

import de.gematik.ti.directory.admin.BaseDirectoryEntry
import de.gematik.ti.directory.admin.DirectoryEntry
import de.gematik.ti.directory.elaborate.elaborate
import io.kotest.core.spec.style.FeatureSpec
import io.kotest.matchers.shouldBe

class TestKind : FeatureSpec({
    feature("Automatic entry kind detection") {
        scenario("Fallback for completely wrong TelematikID") {
            val entry1 =
                DirectoryEntry(
                    directoryEntryBase =
                        BaseDirectoryEntry(
                            telematikID = "89573847563865",
                        ),
                )
            entry1.elaborate().kind shouldBe "Weitere"

            val entry2 =
                DirectoryEntry(
                    directoryEntryBase =
                        BaseDirectoryEntry(
                            telematikID = "",
                        ),
                )
            entry2.elaborate().kind shouldBe "Weitere"
        }
        scenario("Special characters") {
            val entry1 =
                DirectoryEntry(
                    directoryEntryBase =
                        BaseDirectoryEntry(
                            telematikID = "2-2.11.2.1.888888\n",
                        ),
                )
            entry1.elaborate().kind shouldBe "Zahnarztpraxis"
        }
    }
})
