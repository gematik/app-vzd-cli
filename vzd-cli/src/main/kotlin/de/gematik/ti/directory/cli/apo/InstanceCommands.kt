package de.gematik.ti.directory.cli.apo

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.requireObject
import com.github.ajalt.clikt.core.subcommands
import de.gematik.ti.directory.apo.ApoAPI
import de.gematik.ti.directory.apo.ApoCliContext
import de.gematik.ti.directory.apo.ApoClient
import de.gematik.ti.directory.apo.ApoInstance
import de.gematik.ti.directory.global.GlobalAPI

class ApoInstanceCliContext(
    val apoAPI: ApoAPI,
    val instance: ApoInstance,
    val client: ApoClient,
)


class InstanceCommands(inst: ApoInstance) : CliktCommand(name = inst.lowercase(), help = """Commands for ${inst.lowercase()} instance""".trimMargin()) {
    private val context by requireObject<ApoCliContext>()

    init {
        subcommands(
            SearchCommand(),
            ShowCommand(),
        )
    }

    override fun run() {
        val instance = ApoInstance.valueOf(commandName)
        currentContext.obj = ApoInstanceCliContext(
            context.apoAPI,
            instance,
            context.apoAPI.createClient(instance)
        )
    }
}
