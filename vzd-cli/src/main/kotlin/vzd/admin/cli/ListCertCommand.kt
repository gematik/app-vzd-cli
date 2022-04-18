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
import kotlin.io.path.Path
import kotlin.io.path.exists
import kotlin.io.path.useLines

private val logger = KotlinLogging.logger {}


class ListCertCommand : CliktCommand(name = "list-cert", help = "List certificates") {
    private val paramFile: Pair<String, String>? by option("-f", "--param-file",
        help = "Read parameter values from file", metavar = "PARAM FILENAME").pair()
    private val params: Map<String, String> by option("-p", "--param",
        help = "Specify query parameters to find matching entries").associate()
    private val context by requireObject<CommandContext>()

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
        if (params.isEmpty()) {
            throw UsageError("Specify at least one query parameter")
        }

        val result = runBlocking { context.client.readDirectoryCertificates(params) }

        if (context.outputFormat == OutputFormat.CSV) {
            if (context.firstCommand) {
                context.firstCommand = false
                Output.printCsv(UserCertificateCsvHeaders)
            }
        }

        if (context.enableOcsp) {
            result?.forEach {
                it.userCertificate?.let {
                    val ocspResponse = runBlocking { context.pkiClient.ocsp(it) }
                    it.certificateInfo.ocspResponse = ocspResponse
                }
            }
        }

        CertificateOutputMapping[context.outputFormat]?.invoke(params, result)
    }
}
