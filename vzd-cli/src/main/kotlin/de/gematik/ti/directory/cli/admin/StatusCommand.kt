package de.gematik.ti.directory.cli.admin

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.requireObject
import de.gematik.ti.directory.cli.catching
import kotlinx.coroutines.runBlocking

class StatusCommand : CliktCommand(name = "status", help = "Show information about the API") {
    private val context by requireObject<AdminCliContext>()

    override fun run() = catching {
        runBlocking {
            Output.printYaml(context.adminAPI.status(true))
        }
    }
}
