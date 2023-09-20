package de.gematik.ti.directory.cli.admin

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.CliktError
import com.github.ajalt.clikt.core.UsageError
import com.github.ajalt.clikt.core.requireObject
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.switch
import de.gematik.ti.directory.admin.BaseDirectoryEntry
import de.gematik.ti.directory.admin.UpdateBaseDirectoryEntry
import de.gematik.ti.directory.cli.*
import de.gematik.ti.directory.cli.toYamlNoDefaults
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import mu.KotlinLogging
import net.mamoe.yamlkt.Yaml
import java.io.File

private val JsonIgnoreUnknownKeys = Json { ignoreUnknownKeys = true }

class ModifyBaseCommand : CliktCommand(name = "modify-base", help = "Modify single base directory entry") {
    private val logger = KotlinLogging.logger {}
    private val context by requireObject<AdminCliEnvironmentContext>()
    private val file: String? by argument(
        "FILENAME",
        help = "Read the directory BaseDirectoryEntry from specified file, use - to read data from STDIN",
    )
    private val format by option().switch(
        "--json" to RepresentationFormat.JSON,
        "--yaml" to RepresentationFormat.YAML,
    ).default(RepresentationFormat.YAML)

    override fun run() =
        catching {
            val baseFromFile: BaseDirectoryEntry =
                file?.let {
                    when (it) {
                        "-" -> generateSequence(::readLine).joinToString("\n")
                        else -> File(file.toString()).readText(Charsets.UTF_8)
                    }
                }?.let {
                    when (format) {
                        RepresentationFormat.HUMAN, RepresentationFormat.YAML -> Yaml.decodeFromString(it)
                        RepresentationFormat.JSON -> Json.decodeFromString(it)
                        else -> throw CliktError("Unsupported format: $format")
                    }
                } ?: run { throw CliktError("Unable to load base entry") }

            val dn =
                baseFromFile.dn ?: run {
                    val telematikID = baseFromFile.telematikID
                    runBlocking { context.client.readDirectoryEntry(mapOf("telematikID" to telematikID)) }?.first()?.directoryEntryBase?.dn
                        ?: throw CliktError("Entry with telematikID=$telematikID not found")
                }

            logger.debug { "Data will to send to server: $baseFromFile" }

            val jsonData = Json.encodeToString(baseFromFile)
            val updateBaseDirectoryEntry =
                JsonIgnoreUnknownKeys.decodeFromString<UpdateBaseDirectoryEntry>(jsonData)
            runBlocking { context.client.modifyDirectoryEntry(dn.uid, updateBaseDirectoryEntry) }
            val result = runBlocking { context.client.readDirectoryEntry(mapOf("uid" to dn.uid)) }
            val firstEntry = result?.first()?.directoryEntryBase
            when (format) {
                RepresentationFormat.JSON -> echo(firstEntry?.toJsonPrettyNoDefaults())
                RepresentationFormat.HUMAN, RepresentationFormat.YAML -> echo(firstEntry?.toYamlNoDefaults())
                else -> throw UsageError("Cant load for editing in for format: $format")
            }
        }
}
