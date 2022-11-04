package de.gematik.ti.directory.cli.global

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands

class GlobalCommand : CliktCommand(name = "global", help = "Global commands to manage the client itself") {
    init {
        subcommands(ConfigCommand(), UpdateCommand())
    }

    override fun run() = Unit
}
