package de.gematik.ti.directory.cli.admin

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.CliktError
import com.github.ajalt.clikt.core.UsageError
import com.github.ajalt.clikt.core.requireObject
import com.github.ajalt.clikt.parameters.arguments.argument
import de.gematik.ti.directory.admin.BaseDirectoryEntry
import de.gematik.ti.directory.admin.DistinguishedName
import de.gematik.ti.directory.admin.UpdateBaseDirectoryEntry
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import mu.KotlinLogging
import net.mamoe.yamlkt.Yaml
import java.io.File

class ModifyBaseCommand : CliktCommand(name = "modify-base", help = "Modify single base directory entry") {
    private val logger = KotlinLogging.logger {}
    private val file: String? by argument(
        "FILENAME",
        help = "Read the directory BaseDirectoryEntry from specified file, use - to read data from STDIN"
    )
    private val context by requireObject<CommandContext>()

    override fun run() = catching {
        val baseFromFile: BaseDirectoryEntry = file?.let {
            when (it) {
                "-" -> generateSequence(::readLine).joinToString("\n")
                else -> File(file.toString()).readText(Charsets.UTF_8)
            }
        }?.let {
            when (context.outputFormat) {
                OutputFormat.HUMAN, OutputFormat.YAML -> Yaml.decodeFromString(it)
                OutputFormat.JSON -> Json.decodeFromString(it)
                else -> throw CliktError("Unsupported format: ${context.outputFormat}")
            }
        } ?: run { throw CliktError("Unable to load base entry from file") }

        val dn = baseFromFile.dn ?: run {
            DistinguishedName(uid = "123")
            val telematikID = baseFromFile.telematikID
            runBlocking { context.client.readDirectoryEntry(mapOf("telematikID" to telematikID)) }?.first()?.directoryEntryBase?.dn
                ?: throw CliktError("Entry with telematikID=$telematikID not found")
        }

        logger.debug { "Data will to send to server: $baseFromFile" }

        val jsonData = Json.encodeToString(baseFromFile)
        val updateBaseDirectoryEntry =
            Json { ignoreUnknownKeys = true }.decodeFromString<UpdateBaseDirectoryEntry>(jsonData)
        // server bug: when updating telematikID with no certificates the exception is thrown
        updateBaseDirectoryEntry.telematikID = null
        runBlocking { context.client.modifyDirectoryEntry(dn.uid, updateBaseDirectoryEntry) }
        val result = runBlocking { context.client.readDirectoryEntry(mapOf("uid" to dn.uid)) }

        when (context.outputFormat) {
            OutputFormat.JSON -> Output.printJson(result?.first()?.directoryEntryBase)
            OutputFormat.HUMAN, OutputFormat.YAML -> Output.printYaml(result?.first()?.directoryEntryBase)
            else -> throw UsageError("Cant load for editing in for format: ${context.outputFormat}")
        }
    }
}
