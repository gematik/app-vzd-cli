package de.gematik.ti.directory.cli.admin

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.CliktError
import com.github.ajalt.clikt.core.requireObject
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.groups.provideDelegate
import com.github.ajalt.clikt.parameters.options.associate
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.pair
import com.github.ajalt.clikt.parameters.types.int
import com.github.ajalt.clikt.parameters.types.path
import de.gematik.ti.directory.admin.*
import de.gematik.ti.directory.cli.OcspOptions
import de.gematik.ti.directory.cli.catching
import de.gematik.ti.directory.elaborate.DirectoryEntryResourceType
import de.gematik.ti.directory.elaborate.ValidationResult
import de.gematik.ti.directory.elaborate.elaborate
import de.gematik.ti.directory.pki.ExtendedCertificateDataDERSerializer
import de.gematik.ti.directory.pki.OCSPResponseCertificateStatus
import de.gematik.ti.directory.util.escape
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import me.tongfei.progressbar.ProgressBar
import mu.KotlinLogging
import org.bouncycastle.util.encoders.Base64
import kotlin.io.path.Path
import kotlin.io.path.exists
import kotlin.io.path.useLines
import kotlin.io.path.writeBytes
import kotlin.system.measureTimeMillis

private val logger = KotlinLogging.logger {}

var ndjson =
    Json {
        encodeDefaults = true
        serializersModule =
            SerializersModule {
                contextual(ExtendedCertificateDataDERSerializer)
            }
    }

@Serializable
class ElaboratedDumpDirectoryEntry(
    @SerialName("DirectoryEntryBase")
    var directoryEntryBase: BaseDirectoryEntry,
    var userCertificates: List<UserCertificate>? = null,
    @SerialName("Fachdaten")
    var fachdaten: List<Fachdaten>? = null,
    var validationResult: ValidationResult?,
    var kind: String,
    var fhirResourceType: DirectoryEntryResourceType,
)

class DumpCommand : CliktCommand(name = "dump", help = "Create and manage the data dumps") {
    init {
        subcommands(
            DumpCreateCommand(),
            DumpOcspCommand(),
            DumpSaveCert(),
        )
    }

    override fun run() = Unit
}

class DumpCreateCommand : CliktCommand(name = "create", help = "Create dump fetching the data from server") {
    private val context by requireObject<AdminCliEnvironmentContext>()
    private val paramFile: Pair<String, String>? by option(
        "-f",
        "--param-file",
        help = "Read parameter values from file",
        metavar = "PARAM FILENAME",
    ).pair()
    private val customParams: Map<String, String> by option(
        "-p",
        "--param",
        metavar = "PARAM=VALUE",
        help = "Specify query parameters to find matching entries",
    ).associate()
    private val cursorSize by option("-c", "--cursor-size", help = "Size of the cursor per HTTP Request")
        .int()
        .default(500)
    private val expectedTotal by option(
        "-e",
        "--expected-count",
        help = "Expected total count of entries. Used only for cosmetics to display the progressbar.",
    ).int().default(-1)
    private val parameterOptions by ParameterOptions()
    private val ocspOptions by OcspOptions()
    private val kimParameterOptions by KimParameterOptions()

    override fun run() =
        catching {
            val plainParams = parameterOptions.toMap() + customParams
            val kimParams = kimParameterOptions.toMap()

            // cant use both KIM and Query parameters
            if (kimParams.isNotEmpty() && plainParams.isNotEmpty()) {
                throw CliktError("Cannot use both plain and KIM query parameters")
            }

            val query: Pair<Map<String, String>, String> =
                if (kimParams.isNotEmpty()) {
                    Pair(kimParams, ResourceDirectoryEntriesSyncByKim)
                } else {
                    Pair(plainParams, ResourceDirectoryEntriesSync)
                }

            logger.info { "Requesting entries for dump: $query" }
            paramFile?.let { paramFile ->
                val file = Path(paramFile.second)
                if (!file.exists()) throw CliktError("File not found: ${paramFile.second}")
                file.useLines { line ->
                    line.forEach {
                        runQuery(query.first + Pair(paramFile.first, it), query.second)
                    }
                }
            } ?: run {
                runQuery(query.first, query.second)
            }
        }

    private fun expandOcspStatus(entry: DirectoryEntry) {
        if (ocspOptions.enableOcsp) {
            entry.userCertificates?.mapNotNull { it.userCertificate }?.forEach {
                it.certificateInfo.ocspResponse = runBlocking { context.pkiClient.ocsp(it) }
            }
        }
    }

    private fun runQuery(
        params: Map<String, String>,
        resource: String
    ) {
        var entries = 0
        val semaphore = Semaphore(20)
        val elapsed =
            measureTimeMillis {
                val progressBar = ProgressBar("Query $params", expectedTotal.toLong())
                try {
                    runBlocking {
                        context.client.streamDirectoryEntriesPaging(params, cursorSize, resource) { entry ->
                            launch {
                                semaphore.withPermit {
                                    expandOcspStatus(entry)
                                    logger.debug {
                                        "Dumping ${entry.directoryEntryBase.telematikID} (${entry.directoryEntryBase.displayName})"
                                    }
                                    val elaboratedDumpDirectoryEntry = entry.elaborate()
                                    val elaboratedEntry =
                                        ElaboratedDumpDirectoryEntry(
                                            directoryEntryBase = entry.directoryEntryBase,
                                            userCertificates = entry.userCertificates,
                                            fachdaten = entry.fachdaten,
                                            validationResult = elaboratedDumpDirectoryEntry.validationResult,
                                            kind = elaboratedDumpDirectoryEntry.base.kind,
                                            fhirResourceType = elaboratedDumpDirectoryEntry.base.fhirResourceType,
                                        )
                                    println(ndjson.encodeToString(elaboratedEntry))
                                    progressBar.step()
                                    entries++
                                }
                            }
                        }
                    }
                } finally {
                    progressBar.maxHint(progressBar.current)
                    progressBar.close()
                }
            }
        logger.info { "Dumped $entries entries in ${elapsed / 1000} seconds" }
    }
}

class DumpOcspCommand : CliktCommand(name = "ocsp", help = "Make OCSP-Requests for each entry in the dump") {
    private val context by requireObject<AdminCliEnvironmentContext>()

    private val requestDelay by option(
        "--delay",
        help = "Delay between processing entries in milliseconds",
    ).int().default(0)

    private val concurrency by option(
        "--concurrency",
        help = "Maximum number of concurrent OCSP requests",
    ).int().default(20)

    override fun run() =
        catching {
            val semaphore = Semaphore(concurrency)

            var entries = 0
            // initialize lazy variable before coroutines
            context.pkiClient.tsl
            val elapsed =
                measureTimeMillis {
                    runBlocking {
                        System.`in`.bufferedReader().lineSequence().forEach { line ->
                            launch {
                                semaphore.withPermit {
                                    val entry: DirectoryEntry = jsonDirectoryEntryExt.decodeFromString(line)
                                    entries++
                                    logger.debug { "Processing TelematikID: ${entry.directoryEntryBase.telematikID}" }
                                    entry.userCertificates?.mapNotNull { it.userCertificate }?.forEach { cert ->
                                        if (cert.certificateInfo.ocspResponse?.status == OCSPResponseCertificateStatus.GOOD) {
                                            logger.debug { "Certificate already GOOD: ${cert.certificateInfo.serialNumber}" }
                                        } else {
                                            cert.certificateInfo.ocspResponse = context.pkiClient.ocsp(cert)
                                        }
                                    }
                                    println(ndjson.encodeToString(entry))
                                    if (requestDelay > 0) {
                                        delay(requestDelay.toLong())
                                    }
                                }
                            }
                        }
                    }
                }
            logger.info { "Processed $entries entries in ${elapsed / 1000} seconds with concurrency=$concurrency and delay=$requestDelay ms" }
        }
}

class DumpSaveCert :
    CliktCommand(
        name = "save-cert",
        help = "Reads dump from STDIN and saves all X509 certificate to specified directory",
    ) {
    private val context by requireObject<AdminCliEnvironmentContext>()
    private val destination by argument().path(mustExist = true, mustBeWritable = true, canBeFile = false)

    override fun run() =
        catching {
            var entries = 0
            // initialize lazy variable before coroutines
            context.pkiClient.tsl
            val elapsed =
                measureTimeMillis {
                    runBlocking {
                        System.`in`.bufferedReader().lineSequence().forEach { line ->
                            val entry: DirectoryEntry = jsonDirectoryEntryExt.decodeFromString(line)
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
