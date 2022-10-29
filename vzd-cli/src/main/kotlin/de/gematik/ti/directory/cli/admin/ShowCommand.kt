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
        "--human" to OutputFormat.HUMAN,
        "--json" to OutputFormat.JSON,
        "--yaml" to OutputFormat.YAML
    ).default(OutputFormat.HUMAN)
    private val ocspOptions by OcspOptions()
    private val context by requireObject<CommandContext>()
    private val id by argument()

    override fun run() = catching {
        val client = context.client
        val entry = runBlocking { client.readDirectoryEntryByTelematikID(id) }
            ?: throw CliktError("Entry with TelematikID '$id' not found")

        val entryList = listOf(entry)

        if (ocspOptions.enableOcsp) {
            runBlocking { context.adminAPI.expandOCSPStatus(entryList) }
        }

        DirectoryEntryOutputMapping[outputFormat]?.invoke(emptyMap(), entryList)
    }
}
