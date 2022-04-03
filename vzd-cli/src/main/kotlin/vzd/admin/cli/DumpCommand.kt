package vzd.admin.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.CliktError
import com.github.ajalt.clikt.core.requireObject
import com.github.ajalt.clikt.parameters.options.associate
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.pair
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import kotlin.io.path.Path
import kotlin.io.path.exists
import kotlin.io.path.useLines

class DumpCommand : CliktCommand(name = "dump", help = "Dump data from server") {
    private val paramFile: Pair<String, String>? by option("-f", "--param-file",
        help = "Read parameter values from file", metavar = "PARAM FILENAME").pair()
    private val params: Map<String, String> by option("-p", "--param",
        help = "Specify query parameters to find matching entries").associate()
    private val context by requireObject<CommandContext>()
    private val logger = KotlinLogging.logger {}

    override fun run() = catching {
        paramFile?.let { paramFile ->
            val file = Path(paramFile.second)
            if (!file.exists()) throw CliktError("File not found: ${paramFile.second}")
            file.useLines { line ->
                line.forEach {
                    runQuery(params + Pair(paramFile.first, it))
                }
            }
        } ?: run {
            runQuery(params)
        }
    }

    private fun runQuery(params: Map<String, String>) {
        var entries = 0
        runBlocking {
            context.client.streamDirectoryEntries(params) {
                logger.debug { "Dumping ${it.directoryEntryBase.telematikID} (${it.directoryEntryBase.displayName})" }
                Output.printJson(it)
                entries++
            }
        }
        logger.info { "Dumped $entries entries" }
    }
}