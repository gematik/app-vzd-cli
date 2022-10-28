package de.gematik.ti.directory.apo

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import de.gematik.ti.directory.cli.apo.SearchCommand
import de.gematik.ti.directory.cli.apo.ShowCommand
import de.gematik.ti.directory.global.GlobalAPI

class ApoCliContext(
    val apoAPI: ApoAPI
)

class ApoCli : CliktCommand(name = "apo", help = """CLI for ApoVZD API""".trimMargin()) {

    init {
        subcommands(
            ConfigCommand(),
            SearchCommand(),
            ShowCommand()
        )
    }

    override fun run() {
        val apoAPI = ApoAPI(GlobalAPI())
        currentContext.obj = ApoCliContext(apoAPI)
    }
}
