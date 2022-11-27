package de.gematik.ti.directory.cli.admin

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.CliktError
import com.github.ajalt.clikt.core.requireObject
import com.github.ajalt.clikt.parameters.groups.provideDelegate
import com.github.ajalt.clikt.parameters.options.*
import com.github.ajalt.clikt.parameters.types.path
import de.gematik.ti.directory.admin.DirectoryEntry
import de.gematik.ti.directory.cli.OcspOptions
import de.gematik.ti.directory.cli.catching
import kotlinx.coroutines.runBlocking
import java.io.PrintStream
import kotlin.io.path.Path
import kotlin.io.path.exists
import kotlin.io.path.outputStream
import kotlin.io.path.useLines

class ListCommand : CliktCommand(name = "list", help = "List directory entries") {
    private val context by requireObject<AdminCliEnvironmentContext>()
    private val outputFormat by option().switch(
        "--human" to RepresentationFormat.HUMAN,
        "--json" to RepresentationFormat.JSON,
        "--yaml" to RepresentationFormat.YAML,
        "--csv" to RepresentationFormat.CSV,
        "--table" to RepresentationFormat.TABLE,
        "--yaml-ext" to RepresentationFormat.YAML_EXT,
        "--json-ext" to RepresentationFormat.JSON_EXT
    ).default(RepresentationFormat.HUMAN)

    private val paramFile: Pair<String, String>? by option(
        "-f",
        "--param-file",
        help = "Read parameter values from file",
        metavar = "PARAM FILENAME"
    ).pair()
    private val customParams: Map<String, String> by option(
        "-p",
        "--param",
        help = "Specify query parameters to find matching entries",
        metavar = "NAME=VALUE"
    ).associate()
    private val outfile by option("-o", "--outfile", help = "Write output to file").path(mustExist = false, canBeDir = false, canBeFile = true)
    private val parameterOptions by ParameterOptions()
    private val sync by option(help = "use Sync mode").flag()
    private val ocspOptions by OcspOptions()

    override fun run() = catching {
        val params = parameterOptions.toMap() + customParams
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

        val entries = buildList {
            paramFile?.let { paramFile ->
                val file = Path(paramFile.second)
                if (!file.exists()) throw CliktError("File not found: ${paramFile.second}")
                file.useLines { line ->
                    line.forEach {
                        runQuery(params + Pair(paramFile.first, it))?.let { addAll(it) }
                    }
                }
            } ?: run {
                runQuery(params)?.let { addAll(it) }
            }
        }

        val stdout = System.`out`
        try {
            outfile?.let { System.setOut(PrintStream(it.outputStream())) }
            echo(entries.toStringRepresentation(outputFormat))
        } finally {
            System.setOut(stdout)
        }
    }

    private fun runQuery(params: Map<String, String>): List<DirectoryEntry>? {
        val result: List<DirectoryEntry>? = if (sync) {
            runBlocking { context.client.readDirectoryEntryForSync(params) }
        } else {
            runBlocking { context.client.readDirectoryEntry(params) }
        }

        if (ocspOptions.enableOcsp) {
            runBlocking { context.adminAPI.expandOCSPStatus(result) }
        }

        return result
    }
}
