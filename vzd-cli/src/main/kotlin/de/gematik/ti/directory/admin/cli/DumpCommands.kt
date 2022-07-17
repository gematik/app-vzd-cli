package de.gematik.ti.directory.admin.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.CliktError
import com.github.ajalt.clikt.core.requireObject
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.groups.provideDelegate
import com.github.ajalt.clikt.parameters.options.associate
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.pair
import com.github.ajalt.clikt.parameters.types.path
import de.gematik.ti.directory.admin.client.DirectoryEntry
import de.gematik.ti.directory.escape
import de.gematik.ti.directory.pki.OCSPResponseCertificateStatus
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import mu.KotlinLogging
import org.bouncycastle.util.encoders.Base64
import kotlin.io.path.Path
import kotlin.io.path.exists
import kotlin.io.path.useLines
import kotlin.io.path.writeBytes
import kotlin.system.measureTimeMillis

private val jsonExtended = Json {
    serializersModule = optimizedSerializersModule
    encodeDefaults = true
}
private val logger = KotlinLogging.logger {}

class DumpCommand : CliktCommand(name = "dump", help = "Create and manage the data dumps") {
    init {
        subcommands(
            DumpCreateCommand(),
            DumpOcspCommand(),
            DumpSaveCert()
        )
    }

    override fun run() = Unit
}
class DumpCreateCommand : CliktCommand(name = "create", help = "Create dump fetching the data from server") {
    private val paramFile: Pair<String, String>? by option(
        "-f",
        "--param-file",
        help = "Read parameter values from file",
        metavar = "PARAM FILENAME"
    ).pair()
    private val customParams: Map<String, String> by option(
        "-p",
        "--param",
        metavar = "PARAM=VALUE",
        help = "Specify query parameters to find matching entries"
    ).associate()
    private val parameterOptions by ParameterOptions()
    private val context by requireObject<CommandContext>()

    override fun run() = catching {
        val params = parameterOptions.toMap() + customParams
        logger.info { "Requesting entries for dump: $params" }
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
        var entries = 0
        val elapsed = measureTimeMillis {
            runBlocking {
                context.client.streamDirectoryEntries(params) { entry ->
                    if (context.enableOcsp) {
                        entry.userCertificates?.mapNotNull { it.userCertificate }?.forEach {
                            it.certificateInfo.ocspResponse = runBlocking { context.pkiClient.ocsp(it) }
                        }
                    }
                    logger.debug { "Dumping ${entry.directoryEntryBase.telematikID} (${entry.directoryEntryBase.displayName})" }
                    println(jsonExtended.encodeToString(entry))
                    entries++
                }
            }
        }
        logger.info { "Dumped $entries entries in ${elapsed / 1000} seconds" }
    }
}

class DumpOcspCommand : CliktCommand(name = "ocsp", help = "Make OCSP-Requests for each entry in the dump") {
    private val context by requireObject<CommandContext>()

    override fun run() = catching {
        val semaphore = Semaphore(20)

        var entries = 0
        // initialize lazy variable before coroutines
        context.pkiClient.tsl
        val elapsed = measureTimeMillis {
            runBlocking {
                System.`in`.bufferedReader().lineSequence().forEach { line ->
                    launch {
                        semaphore.withPermit {
                            val entry: DirectoryEntry = jsonExtended.decodeFromString(line)
                            entries++
                            logger.debug { "Processing TelematikID: ${entry.directoryEntryBase.telematikID}" }
                            entry.userCertificates?.mapNotNull { it.userCertificate }?.forEach { cert ->
                                if (cert.certificateInfo.ocspResponse?.status == OCSPResponseCertificateStatus.GOOD) {
                                    logger.debug { "Certificate already GOOD: ${cert.certificateInfo.serialNumber}" }
                                } else {
                                    cert.certificateInfo.ocspResponse = context.pkiClient.ocsp(cert)
                                }
                            }
                            println(jsonExtended.encodeToString(entry))
                        }
                    }
                }
            }
        }
        logger.info { "Processed $entries entries in ${elapsed / 1000} seconds" }
    }
}

class DumpSaveCert : CliktCommand(name = "save-cert", help = "Reads dump from STDIN and saves all X509 certificate to specified directory") {
    private val context by requireObject<CommandContext>()
    private val destination by argument().path(mustExist = true, mustBeWritable = true, canBeFile = false)

    override fun run() = catching {
        var entries = 0
        // initialize lazy variable before coroutines
        context.pkiClient.tsl
        val elapsed = measureTimeMillis {
            runBlocking {
                System.`in`.bufferedReader().lineSequence().forEach { line ->
                    val entry: DirectoryEntry = jsonExtended.decodeFromString(line)
                    entries++
                    entry.userCertificates?.mapNotNull { it.userCertificate?.certificateInfo }?.forEach { certInfo ->
                        val filename = "${certInfo.admissionStatement.registrationNumber.escape()}-${certInfo.serialNumber}.der"
                        val path = destination.resolve(filename)
                        path.writeBytes(Base64.decode(certInfo.certData))
                        logger.info { "Written certificate to file ${path.toRealPath()}" }
                    }

                    logger.debug { "Processing TelematikID: ${entry.directoryEntryBase.telematikID}" }
                }
            }
        }
        logger.info { "Processed $entries entries in ${elapsed / 1000} seconds" }
    }
}
