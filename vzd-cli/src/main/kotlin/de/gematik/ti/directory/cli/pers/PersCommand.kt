package de.gematik.ti.directory.cli.pers

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands

class PersCommand : CliktCommand(name = "pers", help = """Process gematik SMC-B/HBA Exports""".trimMargin()) {

    override fun run() = Unit

    init {
        subcommands(
            ExtractCommand()
        )
    }
}
