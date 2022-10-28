package de.gematik.ti.directory.cli.global

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import de.gematik.ti.directory.cli.admin.ConfigGetCommand
import de.gematik.ti.directory.cli.admin.ConfigResetCommand
import de.gematik.ti.directory.cli.admin.ConfigSetCommand

class GlobalCommand : CliktCommand(name = "global", help = "Global commands to manage the client itself") {
    init {
        subcommands(ConfigGetCommand(), ConfigSetCommand(), ConfigResetCommand())
    }

    override fun run() = Unit
}
