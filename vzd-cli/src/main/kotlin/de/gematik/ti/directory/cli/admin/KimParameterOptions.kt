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
