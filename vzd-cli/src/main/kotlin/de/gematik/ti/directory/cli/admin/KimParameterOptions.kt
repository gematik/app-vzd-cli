/*
Copyright 2026, gematik GmbH

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

*******

For additional notes and disclaimer from gematik and in case of changes
by gematik, find details in the "Readme" file.
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
