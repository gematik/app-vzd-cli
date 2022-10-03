package de.gematik.ti.directory.apo

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands

class ApoCli : CliktCommand(name = "apo", help = """CLI for ApoVZD API""".trimMargin()) {

    init {
        subcommands()
    }

    override fun run() = Unit
}
