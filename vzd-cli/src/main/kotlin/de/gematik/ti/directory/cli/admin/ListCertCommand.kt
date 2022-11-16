package de.gematik.ti.directory.cli.admin

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.CliktError
import com.github.ajalt.clikt.core.UsageError
import com.github.ajalt.clikt.core.requireObject
import com.github.ajalt.clikt.parameters.groups.provideDelegate
import com.github.ajalt.clikt.parameters.options.*
import com.github.ajalt.clikt.parameters.types.path
import de.gematik.ti.directory.admin.UserCertificate
import de.gematik.ti.directory.cli.OcspOptions
import de.gematik.ti.directory.cli.catching
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import java.io.PrintStream
import kotlin.io.path.Path
import kotlin.io.path.exists
import kotlin.io.path.outputStream
import kotlin.io.path.useLines

private val logger = KotlinLogging.logger {}

class ListCertCommand : CliktCommand(name = "list-cert", help = "List certificates") {
    private val context by requireObject<AdminCliEnvironmentContext>()
    private val outputFormat by option().switch(
        "--human" to OutputFormat.HUMAN,
        "--json" to OutputFormat.JSON,
        "--yaml" to OutputFormat.YAML,
        "--csv" to OutputFormat.CSV,
        "--table" to OutputFormat.TABLE
    ).default(OutputFormat.TABLE)
    private val paramFile: Pair<String, String>? by option(
        "-f",
        "--param-file",
        help = "Read parameter values from file",
        metavar = "PARAM FILENAME"
    ).pair()
    private val customParams: Map<String, String> by option(
        "-p",
        "--param",
        help = "Specify query parameters to find matching entries"
    ).associate()
    private val outfile by option("-o", "--outfile", help = "Write output to file").path(mustExist = false, canBeDir = false, canBeFile = true)
    private val parameterOptions by ParameterOptions()
    private val ocspOptions by OcspOptions()

    override fun run() = catching {
        val params = parameterOptions.toMap() + customParams

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

            if (outputFormat == OutputFormat.CSV) {
                print('\uFEFF')
                Output.printCsv(UserCertificateCsvHeaders)
            }
            CertificateOutputMapping[outputFormat]?.invoke(params, entries)
        } finally {
            System.setOut(stdout)
        }
    }

    private fun runQuery(params: Map<String, String>): List<UserCertificate>? {
        if (params.isEmpty()) {
            throw UsageError("Specify at least one query parameter")
        }

        val result = runBlocking { context.client.readDirectoryCertificates(params) }

        if (ocspOptions.enableOcsp) {
            result?.forEach {
                it.userCertificate?.let {
                    val ocspResponse = runBlocking { context.pkiClient.ocsp(it) }
                    it.certificateInfo.ocspResponse = ocspResponse
                }
            }
        }

        return result
    }
}
