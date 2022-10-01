package de.gematik.ti.directory.cli.admin

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.UsageError
import com.github.ajalt.clikt.core.requireObject
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.multiple
import com.github.ajalt.clikt.parameters.types.path
import de.gematik.ti.directory.util.CertificateDataDER
import kotlinx.coroutines.runBlocking
import org.bouncycastle.util.encoders.Base64
import kotlin.io.path.inputStream

class CertInfoCommand : CliktCommand(name = "cert-info", help = "Show details of a certificate") {
    private val files by argument().path(mustBeReadable = true, canBeDir = false).multiple()
    private val context by requireObject<CommandContext>()
    override fun run() = catching {
        files.forEach {
            val certB64 = Base64.toBase64String(it.inputStream().readBytes())
            val userCertificate = CertificateDataDER(certB64)
            val certificateInfo = userCertificate.certificateInfo

            if (context.enableOcsp) {
                certificateInfo.ocspResponse = runBlocking { context.pkiClient.ocsp(userCertificate) }
            }

            when (context.outputFormat) {
                OutputFormat.JSON -> Output.printJson(certificateInfo)
                OutputFormat.HUMAN, OutputFormat.YAML -> Output.printYaml(certificateInfo)
                else -> throw UsageError("Cant use output format: ${context.outputFormat}")
            }
        }
    }
}
