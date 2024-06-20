package de.gematik.ti.directory.cli.fhir

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands

class FdvCommands : CliktCommand(name = "fdv", help = "Commands for FDVSearchAPI") {
    init {
        subcommands(
            FdvLoginCommand(),
            FdvTokenCommand(),
            SearchCommand { context, query -> context.client.searchFdv(query) },
            FdvShowCommand(),
        )
    }

    override fun run() {
        // no-op
    }
}
