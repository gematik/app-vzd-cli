package de.gematik.ti.directory.apo

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.requireObject
import com.github.ajalt.clikt.core.subcommands
import de.gematik.ti.directory.cli.CliContext
import de.gematik.ti.directory.cli.apo.ApoAPI
import de.gematik.ti.directory.cli.apo.ApoInstance
import de.gematik.ti.directory.cli.apo.InstanceCommands

class ApoCliContext(
    val apoAPI: ApoAPI,
)

class ApoCli : CliktCommand(name = "apo", help = """CLI for ApoVZD API""".trimMargin()) {
    private val context by requireObject<CliContext>()

    init {
        subcommands(
            ConfigCommand(),
        )
        subcommands(
            ApoInstance.values().map {
                InstanceCommands(it)
            },
        )
    }

    override fun run() {
        val apoAPI = ApoAPI(context.globalAPI)
        currentContext.obj = ApoCliContext(apoAPI)
    }
}
