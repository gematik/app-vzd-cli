package de.gematik.ti.directory.cli

import com.github.ajalt.clikt.parameters.groups.OptionGroup
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option

class OcspOptions : OptionGroup("OCSP options") {
    val enableOcsp: Boolean by option(
        "--ocsp",
        help = "Validate certificates using OCSP",
    ).flag()
}
