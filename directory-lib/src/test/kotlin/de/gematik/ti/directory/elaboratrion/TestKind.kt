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

package de.gematik.ti.directory.elaboratrion

import de.gematik.ti.directory.admin.BaseDirectoryEntry
import de.gematik.ti.directory.admin.DirectoryEntry
import de.gematik.ti.directory.elaborate.elaborate
import io.kotest.core.spec.style.FeatureSpec
import io.kotest.matchers.shouldBe

class TestKind :
    FeatureSpec({
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
