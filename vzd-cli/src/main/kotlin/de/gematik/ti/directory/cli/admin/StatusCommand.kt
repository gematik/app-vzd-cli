package de.gematik.ti.directory.cli.admin

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.requireObject
import de.gematik.ti.directory.cli.catching
import de.gematik.ti.directory.cli.toYaml
import kotlinx.coroutines.runBlocking

class StatusCommand : CliktCommand(name = "status", help = "Show information about the API") {
    private val context by requireObject<AdminCliContext>()

    override fun run() =
        catching {
            runBlocking {
                echo(context.adminAPI.status(true).toYaml())
            }
        }
}
