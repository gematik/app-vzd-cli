package vzd.admin.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.UsageError
import com.github.ajalt.clikt.core.requireObject
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.multiple
import com.github.ajalt.clikt.parameters.types.path
import mu.KotlinLogging
import org.bouncycastle.util.encoders.Base64
import vzd.admin.client.CertificateDataDER
import vzd.admin.client.toCertificateInfo
import kotlin.io.path.inputStream

class CertInfoCommand : CliktCommand(name = "cert-info", help = "Show details of a certificate") {
    private val logger = KotlinLogging.logger {}
    private val files by argument().path(mustBeReadable = true).multiple()
    private val context by requireObject<CommandContext>()
    override fun run() = catching {
        files.forEach {
            val certB64 = Base64.toBase64String(it.inputStream().readBytes())
            val userCertificate = CertificateDataDER(certB64)
            val certificateInfo = userCertificate.toCertificateInfo()

            when (context.outputFormat) {
                OutputFormat.JSON -> Output.printJson(certificateInfo)
                OutputFormat.HUMAN, OutputFormat.YAML -> Output.printYaml(certificateInfo)
                else -> throw UsageError("Cant use output format: ${context.outputFormat}")
            }
        }

    }
}
