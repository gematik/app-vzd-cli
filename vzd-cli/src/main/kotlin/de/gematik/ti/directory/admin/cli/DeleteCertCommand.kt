package de.gematik.ti.directory.admin.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.CliktError
import com.github.ajalt.clikt.core.UsageError
import com.github.ajalt.clikt.core.requireObject
import com.github.ajalt.clikt.parameters.groups.provideDelegate
import com.github.ajalt.clikt.parameters.options.*
import com.github.ajalt.clikt.parameters.types.path
import de.gematik.ti.directory.escape
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import net.mamoe.yamlkt.Yaml
import org.bouncycastle.util.encoders.Base64
import java.nio.file.Paths
import kotlin.io.path.writeBytes

class DeleteCertCommand : CliktCommand(name = "delete-cert", help = "Delete certificates") {
    private val logger = KotlinLogging.logger {}
    private val customParams: Map<String, String> by option(
        "-p",
        "--param",
        help = "Specify query parameters to find matching entries",
        metavar = "PARAM=VALUE"
    ).associate()
    private val parameterOptions by ParameterOptions()

    private val backupDir by option(
        "-b",
        "--backup-dir",
        metavar = "OUTPUT_DIR",
        help = "Backup directory to store deleted certificates."
    )
        .path(mustExist = true, canBeFile = false, mustBeWritable = true)
        .default(Paths.get(""))

    private val match by option("-m", "--match", help = "Pattern to match the to be deleted certificates.").multiple()

    private val dryRun by option("--dry", help = "Perform a dry run and not delete any certificate.").flag()

    // private val force by option("-f", "--force").flag(default = false)
    private val context by requireObject<CommandContext>()

    override fun run() = catching {
        val params = parameterOptions.toMap() + customParams
        if (params.isEmpty()) {
            throw UsageError("Specify at least one query parameter")
        }

        val result = runBlocking { context.client.readDirectoryEntry(params) }

        if (result.isNullOrEmpty()) {
            logger.info { "Entry not found: $params" }
            throw CliktError("Entry not found: $params")
        }

        // Make sure we clear only one Entry per run. Just to be safe.
        if (result.size > 1) {
            logger.error { "Query matches too many entries: $params" }
            throw CliktError("Query matches too many entries: $params")
        }

        val entry = result.first()

        echo("Processing entry ${entry.directoryEntryBase.telematikID} ${entry.directoryEntryBase.displayName}")
        var deletedCount = 0
        entry.userCertificates?.filter { it.userCertificate?.certificateInfo != null }?.forEach certLoop@{ userCertificate ->
            val cert = userCertificate.userCertificate!!.certificateInfo
            val certText = Yaml.encodeToString(cert)
            val matches = if (match.isNotEmpty()) {
                match.firstOrNull {
                    // val matches = Regex(it, RegexOption.MULTILINE).matches(certText)
                    val matches = Regex(it, setOf(RegexOption.IGNORE_CASE, RegexOption.MULTILINE)).containsMatchIn(certText)

                    if (matches) {
                        logger.debug { "Found matching certificate: $it" }
                    }

                    matches
                } != null
            } else {
                true
            }
            if (!matches) {
                echo("Skipping certificate: ${cert.serialNumber}")
                return@certLoop
            }
            logger.debug { "Deleting certificate: $certText" }
            echo("Deleting certificate ${cert.serialNumber} ${cert.subjectInfo.serialNumber ?: ""}")
            backupDir.let {
                val filename = "${cert.admissionStatement.registrationNumber.escape()}-${cert.serialNumber}.der"
                val path = backupDir.resolve(filename)
                path.writeBytes(Base64.decode(cert.certData))
                logger.info { "Written certificate to file ${path.toRealPath()}" }
            }
            deletedCount += 1
            if (dryRun) {
                logger.debug { "Dry run: not deleting the certificate" }
            } else {
                runBlocking { context.client.deleteDirectoryEntryCertificate(userCertificate.dn?.uid!!, userCertificate.dn?.cn!!) }
            }
        }

        if (dryRun) {
            echo("Would have deleted $deletedCount of ${entry.userCertificates?.count()} certificates (dry run)")
        } else {
            echo("Deleted $deletedCount of ${entry.userCertificates?.count()} certificates")
        }
    }
}
