package vzd.admin.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.CliktError
import com.github.ajalt.clikt.core.requireObject
import com.github.ajalt.clikt.parameters.options.associate
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.pair
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import mu.KotlinLogging
import kotlin.io.path.Path
import kotlin.io.path.exists
import kotlin.io.path.useLines
import kotlin.system.measureTimeMillis

class DumpCommand : CliktCommand(name = "dump", help = "Dump data from server") {
    val jsonExtended = Json {
        serializersModule = optimizedSerializersModule
    }
    private val paramFile: Pair<String, String>? by option("-f", "--param-file",
        help = "Read parameter values from file", metavar = "PARAM FILENAME").pair()
    private val params: Map<String, String> by option("-p", "--param",
        metavar = "PARAM=VALUE",
        help = "Specify query parameters to find matching entries").associate()
    private val context by requireObject<CommandContext>()
    private val logger = KotlinLogging.logger {}
    private val json = Json { }

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
        var entries = 0
        val elapsed = measureTimeMillis {
            runBlocking {
                context.client.streamDirectoryEntries(params) {
                    if (context.enableOcsp) {
                        it.userCertificates?.mapNotNull { it.userCertificate }?.forEach {
                            it.certificateInfo.ocspResponse = runBlocking { context.pkiClient.ocsp(it) }
                        }
                    }
                    logger.debug { "Dumping ${it.directoryEntryBase.telematikID} (${it.directoryEntryBase.displayName})" }
                    echo(jsonExtended.encodeToString(it))
                    entries++
                }
            }
        }
        logger.info { "Dumped $entries entries in ${elapsed/1000} seconds" }
    }
}