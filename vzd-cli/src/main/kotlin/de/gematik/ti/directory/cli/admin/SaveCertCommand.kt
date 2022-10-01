package de.gematik.ti.directory.cli.admin

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.CliktError
import com.github.ajalt.clikt.core.UsageError
import com.github.ajalt.clikt.core.requireObject
import com.github.ajalt.clikt.parameters.groups.provideDelegate
import com.github.ajalt.clikt.parameters.options.*
import com.github.ajalt.clikt.parameters.types.path
import de.gematik.ti.directory.cli.escape
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.bouncycastle.util.encoders.Base64
import java.nio.file.Paths
import kotlin.io.path.Path
import kotlin.io.path.exists
import kotlin.io.path.useLines
import kotlin.io.path.writeBytes

private val logger = KotlinLogging.logger {}

class SaveCertCommand : CliktCommand(name = "save-cert", help = "Saves certificate to DER files") {
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
    private val parameterOptions by ParameterOptions()
    private val outputDir by option("-o", "--output-dir", metavar = "OUTPUT_DIR", help = "Output directory for certificate files. Default ist current directory.")
        .path(mustExist = true, canBeFile = false)
        .default(Paths.get(""))
    private val context by requireObject<CommandContext>()

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
    }

    private fun runQuery(params: Map<String, String>) {
        if (params.isEmpty()) {
            throw UsageError("Specify at least one query parameter")
        }

        val result = runBlocking { context.client.readDirectoryCertificates(params) }

        result?.forEach {
            val cert = it.userCertificate?.certificateInfo ?: return
            val filename = "${cert.admissionStatement.registrationNumber.escape()}-${cert.serialNumber}.der"
            val path = outputDir.resolve(filename)
            path.writeBytes(Base64.decode(it.userCertificate?.base64String))
            logger.info { "Written certificate to file ${path.toRealPath()}" }
        }

        echo("Written ${result?.size} certificates to ${outputDir.toRealPath()}")
    }
}
