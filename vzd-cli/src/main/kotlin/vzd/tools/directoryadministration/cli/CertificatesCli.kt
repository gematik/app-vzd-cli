package vzd.tools.directoryadministration.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.UsageError
import com.github.ajalt.clikt.core.requireObject
import com.github.ajalt.clikt.parameters.options.associate
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.choice
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import vzd.tools.directoryadministration.Client
import vzd.tools.directoryadministration.UserCertificate
import vzd.tools.directoryadministration.toCertificateInfo

private val logger = KotlinLogging.logger {}

val CertificateOutputMapping = mapOf(
    "yaml" to { value: List<UserCertificate>? -> Output.printYamlOptimized(value) },
    "json" to { value: List<UserCertificate>?-> Output.printJson(value) },
    "json-ext" to { value: List<UserCertificate>?-> Output.printJsonOptimized(value) },
    "list" to { value: List<UserCertificate>? ->
        value?.forEach {
            val cert = it.userCertificate?.toCertificateInfo()
            println("${it.dn?.uid} ${it.telematikID} ${it.entryType} ${cert?.publicKeyAlgorithm} ${cert?.subject}")
        }
    },
)

class ListCertificates: CliktCommand(name = "list-cert", help="List certificates") {
    private val params: Map<String, String> by option("-Q", "--query",
        help="Specify query parameters to find matching entries").associate()
    private val context by requireObject<CommandContext>();

    override fun run() = catching {
        if (params.isEmpty()) {
            throw UsageError("Specify at least one query parameter")
        }

        val result = runBlocking { context.client.readDirectoryCertificates(params) }

        CertificateOutputMapping[context.output]?.invoke(result)
    }
}

class AddCertificate: CliktCommand(name = "add-cert", help="Add certificate") {
    private val client by requireObject<Client>();
    override fun run() = catching {
        TODO("Not yet implemented")
    }
}

class DeleteCertificates: CliktCommand(name = "delete-cert", help="Delete certificates") {
    override fun run() = catching {
        TODO("Not yet implemented")
    }
}