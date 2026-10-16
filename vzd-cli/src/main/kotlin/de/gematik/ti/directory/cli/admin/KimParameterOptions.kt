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

package de.gematik.ti.directory.cli.admin

import com.github.ajalt.clikt.parameters.groups.OptionGroup
import com.github.ajalt.clikt.parameters.options.option

class KimParameterOptions : OptionGroup(name = "KIM Query parameters") {
    val mail by option("--kim-mail", help = "Mail address to search for. Supports wildcards.")
    val data by option("--kim-data", help = "KIM data to search for. Supports wildcards.")

    fun toMap(): Map<String, String> {
        val self = this
        return buildMap<String, String> {
            if (self.mail != null) {
                put("mail", self.mail!!)
            }
            if (self.data != null) {
                put("kimData", self.data!!)
            }
        }
    }
}
