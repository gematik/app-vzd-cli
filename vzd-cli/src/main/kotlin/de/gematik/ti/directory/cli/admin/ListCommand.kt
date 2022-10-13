package de.gematik.ti.directory.cli.admin

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.CliktError
import com.github.ajalt.clikt.core.requireObject
import com.github.ajalt.clikt.parameters.groups.provideDelegate
import com.github.ajalt.clikt.parameters.options.*
import de.gematik.ti.directory.admin.DirectoryEntry
import kotlinx.coroutines.runBlocking
import kotlin.io.path.Path
import kotlin.io.path.exists
import kotlin.io.path.useLines

class ListCommand : CliktCommand(name = "list", help = "List directory entries") {
    private val outputFormat by option().switch(
        "--human" to OutputFormat.HUMAN,
        "--json" to OutputFormat.JSON,
        "--yaml" to OutputFormat.YAML,
        "--csv" to OutputFormat.CSV,
        "--table" to OutputFormat.TABLE
    )

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
    private val parameterOptions by ParameterOptions()
    private val context by requireObject<CommandContext>()
    private val sync by option(help = "use Sync mode").flag()

    override fun run() = catching {
        context.outputFormat = outputFormat ?: context.outputFormat
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
    }

    private fun runQuery(params: Map<String, String>) {
        val result: List<DirectoryEntry>? = if (sync) {
            runBlocking { context.client.readDirectoryEntryForSync(params) }
        } else {
            runBlocking { context.client.readDirectoryEntry(params) }
        }

        if (context.outputFormat == OutputFormat.CSV) {
            if (context.firstCommand) {
                context.firstCommand = false
                print('\uFEFF')
                Output.printCsv(DirectoryEntryCsvHeaders)
            }
        }

        if (context.enableOcsp) {
            result?.mapNotNull { it.userCertificates }
                ?.flatten()
                ?.mapNotNull { it.userCertificate }
                ?.forEach {
                    val ocspResponse = runBlocking { context.pkiClient.ocsp(it) }
                    it.certificateInfo.ocspResponse = ocspResponse
                }
        }

        DirectoryEntryOutputMapping[context.outputFormat]?.invoke(params, result)
    }
}
