package de.gematik.ti.directory.cli.bff

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands

class BffCommand : CliktCommand(name = "bff", help = """CLI to run client as microservice backend for frontend""".trimMargin()) {
    override fun run() {}

    init {
        subcommands(
            BffStartCommand(),
        )
    }
}
