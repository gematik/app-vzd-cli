package de.gematik.ti.directory.cli.service

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands

class ServiceCommand : CliktCommand(name = "service", help = """CLI to run client as microservice""".trimMargin()) {
    override fun run() {}

    init {
        subcommands(
            ServiceRunCommand(),
        )
    }
}
