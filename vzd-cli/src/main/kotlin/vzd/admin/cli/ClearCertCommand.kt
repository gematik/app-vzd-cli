package vzd.admin.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.CliktError
import com.github.ajalt.clikt.core.UsageError
import com.github.ajalt.clikt.core.requireObject
import com.github.ajalt.clikt.parameters.options.associate
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.pair
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import vzd.admin.client.toCertificateInfo
import kotlin.io.path.Path
import kotlin.io.path.exists
import kotlin.io.path.useLines

class ClearCertCommand : CliktCommand(name = "clear-cert", help = "Clear all certificates of a given entry") {
    private val logger = KotlinLogging.logger {}
    private val paramFile: Pair<String, String>? by option("-f", "--param-file",
        help = "Read parameter values from file", metavar = "PARAM FILENAME").pair()
    private val params: Map<String, String> by option("-p", "--param",
        help = "Specify query parameters to find matching entries", metavar = "PARAM=VALUE").associate()

    //private val force by option("-f", "--force").flag(default = false)
    private val context by requireObject<CommandContext>()

    override fun run() = catching {
        paramFile?.let { paramFile ->
            val file = Path(paramFile.second)
            if (!file.exists()) throw CliktError("File not found: ${paramFile.second}")
            file.useLines { line ->
                line.forEach {
                    runSingleCommand(params + Pair(paramFile.first, it))
                }
            }
        } ?: run {
            runSingleCommand(params)
        }
    }

    private fun runSingleCommand(params: Map<String, String>) {
        if (params.isEmpty()) {
            throw UsageError("Specify at least one query parameter")
        }

        val result = runBlocking { context.client.readDirectoryEntry(params) }

        if (result.isNullOrEmpty()) {
            logger.info { "Entry not found: $params" }
            throw CliktError("Entry not found: $params")
        }

        // Make sure we clear only one Entry per run. Just to be safe.
        if (result?.size > 1) {
            logger.error { "Query matches too many entries: $params" }
            throw CliktError("Query matches too many entries: $params")
        }

        val entry = result.first()

        logger.info { "Clearing ${entry.userCertificates?.size} certificate(s) in ${entry.directoryEntryBase.telematikID}" }

        entry.userCertificates?.forEach {
            logger.debug { "Deleting certificate: ${it.userCertificate?.base64String}" }
            logger.debug { it.dn }
            echo("Deleting certificate: telematikID=${it.telematikID} serialNumber=${it.userCertificate?.toCertificateInfo()?.serialNumber}")
            //runBlocking { context.client.deleteDirectoryEntryCertificate(it.dn?.uid!!, it.dn?.cn!!) }
        }
    }

}
