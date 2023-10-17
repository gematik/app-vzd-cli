package de.gematik.ti.directory.cli.admin

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.CliktError
import com.github.ajalt.clikt.core.requireObject
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.groups.provideDelegate
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.switch
import de.gematik.ti.directory.admin.readDirectoryEntryByTelematikID
import de.gematik.ti.directory.cli.OcspOptions
import de.gematik.ti.directory.cli.catching
import kotlinx.coroutines.runBlocking

class ShowCommand : CliktCommand(name = "show", help = "Show all information about an entry") {
    private val outputFormat by option().switch(
        "--human" to RepresentationFormat.HUMAN,
        "--json" to RepresentationFormat.JSON,
        "--yaml" to RepresentationFormat.YAML,
        "--yaml-ext" to RepresentationFormat.YAML_EXT,
        "--json-ext" to RepresentationFormat.JSON_EXT,
    ).default(RepresentationFormat.HUMAN)
    private val ocspOptions by OcspOptions()
    private val context by requireObject<AdminCliEnvironmentContext>()
    private val id by argument()

    override fun run() =
        catching {
            val client = context.client
            val entry =
                runBlocking { client.readDirectoryEntryByTelematikID(id) }
                    ?: throw CliktError("Entry with TelematikID '$id' not found")

            if (ocspOptions.enableOcsp) {
                runBlocking { context.adminAPI.expandOCSPStatus(listOf(entry)) }
            }

            echo(entry.toStringRepresentation(outputFormat))
        }
}
