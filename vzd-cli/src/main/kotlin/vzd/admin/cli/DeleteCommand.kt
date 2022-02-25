package vzd.admin.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.UsageError
import com.github.ajalt.clikt.core.requireObject
import com.github.ajalt.clikt.parameters.options.associate
import com.github.ajalt.clikt.parameters.options.option
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging

class DeleteCommand: CliktCommand(name="delete", help="Delete specified directory entries") {
    private val logger = KotlinLogging.logger {}
    private val params: Map<String, String> by option("-p", "--param",
        help="Specify query parameters to find matching entries").associate()
    //val force by option(help="Force delete").flag()
    private val context by requireObject<CommandContext>()

    override fun run() = catching {
        runBlocking {
            if (params.isEmpty()) {
                throw UsageError("Specify at least one query parameter")
            }
            val result = context.client.readDirectoryEntry(params)
            result?.forEach {
                val answer = prompt("Type YES to delete '${it.directoryEntryBase.telematikID}' '${it.directoryEntryBase.displayName}': ")
                if (answer == "YES") {
                    logger.debug { "Deleting '${it.directoryEntryBase.displayName}' '${it.directoryEntryBase.dn?.uid}'" }
                    if (it.directoryEntryBase.dn?.uid != null) {
                        context.client.deleteDirectoryEntry( it.directoryEntryBase.dn!!.uid )
                    }
                }
            }
        }

    }
}