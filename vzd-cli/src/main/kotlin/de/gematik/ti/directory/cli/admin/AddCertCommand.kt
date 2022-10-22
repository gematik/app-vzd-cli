package de.gematik.ti.directory.cli.admin

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.CliktError
import com.github.ajalt.clikt.core.requireObject
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.multiple
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.path
import de.gematik.ti.directory.admin.AdminResponseException
import de.gematik.ti.directory.admin.UserCertificate
import de.gematik.ti.directory.util.CertificateDataDER
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import net.mamoe.yamlkt.Yaml
import org.bouncycastle.util.encoders.Base64
import kotlin.io.path.inputStream

class AddCertCommand : CliktCommand(name = "add-cert", help = "Add certificate to existing DirectoryEntry") {
    private val logger = KotlinLogging.logger {}

    // val inputFormat by option("--inform", "-i").choice("der", "pem")
    private val files by argument().path(mustBeReadable = true).multiple()
    private val ignore by option("--ignore", "-i", help = "Ignore Error 409 (certificate exists).").flag()

    private val context by requireObject<CommandContext>()
    override fun run() = catching {
        files.forEach {
            val certB64 = Base64.toBase64String(it.inputStream().readBytes())
            val certDER = CertificateDataDER(certB64)

            val userCertificate = UserCertificate(userCertificate = certDER, telematikID = certDER.certificateInfo.admissionStatement.registrationNumber)
            logger.info { "Adding Certificate ${Yaml.encodeToString(userCertificate.userCertificate?.certificateInfo)}" }

            val entries = runBlocking {
                context.client.readDirectoryEntry(mapOf("telematikID" to certDER.certificateInfo.admissionStatement.registrationNumber))
            }

            entries?.first()?.let {
                logger.info { "Found matching Entry: ${it.directoryEntryBase.dn?.uid} ${it.directoryEntryBase.displayName}" }
                runBlocking {
                    try {
                        context.client.addDirectoryEntryCertificate(
                            it.directoryEntryBase.dn?.uid!!,
                            userCertificate
                        )
                    } catch (e: AdminResponseException) {
                        if (!ignore || e.response.status != HttpStatusCode.Conflict) {
                            throw e
                        }
                        logger.warn { "Certificate with serialNumber=${userCertificate.userCertificate?.certificateInfo?.serialNumber} already exists. Ignoring conflict." }
                    }
                }
            }
                ?: run { throw CliktError("Entry with telematikID ${certDER.certificateInfo.admissionStatement.registrationNumber} not found.") }
        }
    }
}
