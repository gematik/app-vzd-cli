package de.gematik.ti.directory.cli

import com.github.ajalt.clikt.parameters.groups.OptionGroup
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option

class ProxyOptions : OptionGroup("Proxy options") {
    val enableProxy: Boolean? by option(
        "-x",
        "--proxy-on",
        help = "Forces the use of the proxy, overrides the configuration"
    )
        .flag("--proxy-off", "-X")
}

class OcspOptions : OptionGroup("OCSP options") {
    val enableOcsp: Boolean by option(
        "-o",
        "--ocsp",
        help = "Validate certificates using OCSP"
    )
        .flag()
}
