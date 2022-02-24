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
import vzd.admin.client.UserCertificate
import vzd.admin.client.toCertificateInfo
import kotlin.io.path.Path
import kotlin.io.path.exists
import kotlin.io.path.useLines

private val logger = KotlinLogging.logger {}

private val CsvHeaders = listOf(
    "query",
    "uid",
    "telematikID",
    "entryType",
    "publicKeyAlgorithm",
    "subject",
    "notBefore",
    "notAfter",
)

val CertificateOutputMapping = mapOf(
    OutputFormat.HUMAN to { _: Map<String, String>, value: List<UserCertificate>? -> Output.printHuman(value) },
    OutputFormat.YAML to { _: Map<String, String>, value: List<UserCertificate>? -> Output.printYaml(value) },
    OutputFormat.JSON to { _: Map<String, String>, value: List<UserCertificate>?-> Output.printJson(value) },
    OutputFormat.SHORT to { _: Map<String, String>, value: List<UserCertificate>? ->
        value?.forEach {
            val cert = it.userCertificate?.toCertificateInfo()
            println("${it.dn?.uid} ${it.telematikID} ${it.entryType} ${cert?.publicKeyAlgorithm} ${cert?.subject}")
        }
    },
    OutputFormat.CSV to { query: Map<String, String>, value: List<UserCertificate>? ->

        value?.forEach {
            val cert = it.userCertificate?.toCertificateInfo()
            Output.printCsv(listOf(
                query.toString(),
                it.dn?.uid,
                it.telematikID,
                it.entryType,
                cert?.publicKeyAlgorithm,
                cert?.subject,
                cert?.notBefore,
                cert?.notAfter,

                ))
        }

        if (value == null || value.isEmpty()) {
            Output.printCsv(listOf(query.toString(), "Not Found"))
        }

    },

)

class ListCertificates: CliktCommand(name = "list-cert", help="List certificates") {
    private val paramFile: Pair<String, String>? by option("-f", "--param-file",
        help="Read parameter values from file", metavar = "PARAM FILENAME").pair()
    private val params: Map<String, String> by option("-p", "--param",
        help="Specify query parameters to find matching entries").associate()
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
                Output.printCsv(CsvHeaders)
            }
        }

        CertificateOutputMapping[context.outputFormat]?.invoke(params, result)
    }
}

class AddCertificate: CliktCommand(name = "add-cert", help="Add certificate") {
    override fun run() = catching {
        TODO("Not yet implemented")
    }
}

class DeleteCertificates: CliktCommand(name = "delete-cert", help="Delete certificates") {
    override fun run() = catching {
        TODO("Not yet implemented")
    }
}