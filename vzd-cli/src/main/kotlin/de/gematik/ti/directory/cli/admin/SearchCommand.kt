package de.gematik.ti.directory.cli.admin

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.requireObject
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.multiple
import de.gematik.ti.directory.admin.DirectoryEntry
import de.gematik.ti.directory.admin.quickSearch
import de.gematik.ti.directory.cli.catching
import kotlinx.coroutines.runBlocking

class SearchCommand : CliktCommand(name = "search", help = "Search for directory entries") {
    private val arguments by argument().multiple()
    private val context by requireObject<AdminCliEnvironmentContext>()

    override fun run() = catching {
        val queryString = arguments.joinToString(" ")
        val result: List<DirectoryEntry> = runBlocking {
            context.client.quickSearch(queryString).directoryEntries
        }

        DirectoryEntryOutputMapping[OutputFormat.TABLE]?.invoke(mapOf("query" to queryString), result)
    }
}
