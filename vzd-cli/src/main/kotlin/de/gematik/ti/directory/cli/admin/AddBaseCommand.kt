package de.gematik.ti.directory.cli.admin

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.CliktError
import com.github.ajalt.clikt.core.UsageError
import com.github.ajalt.clikt.core.requireObject
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.optional
import com.github.ajalt.clikt.parameters.options.associate
import com.github.ajalt.clikt.parameters.options.deprecated
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.path
import de.gematik.ti.directory.admin.AdminResponseException
import de.gematik.ti.directory.admin.BaseDirectoryEntry
import de.gematik.ti.directory.admin.CreateDirectoryEntry
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import mu.KotlinLogging
import net.mamoe.yamlkt.Yaml
import java.io.File
import kotlin.reflect.KMutableProperty
import kotlin.reflect.full.memberProperties
import kotlin.reflect.typeOf

fun setAttributes(baseDirectoryEntry: BaseDirectoryEntry?, attrs: Map<String, String>) {
    attrs.forEach { (name, value) ->

        val property = BaseDirectoryEntry::class.memberProperties
            .filterIsInstance<KMutableProperty<*>>()
            .first { it.name == name }

        if (property.returnType == typeOf<String>() || property.returnType == typeOf<String?>()) {
            property.setter.call(baseDirectoryEntry, value)
        } else if (property.returnType == typeOf<Int>() || property.returnType == typeOf<Int?>()) {
            property.setter.call(baseDirectoryEntry, value.toInt())
        } else if (property.returnType == typeOf<List<String>>() || property.returnType == typeOf<List<String>?>()) {
            property.setter.call(baseDirectoryEntry, value.split(',').map { it.trim() })
        } else {
            throw UsageError("Unsupported property type '$name': ${property.returnType}")
        }
    }
}

class AddBaseCommand : CliktCommand(name = "add-base", help = "Add new directory entry") {
    private val logger = KotlinLogging.logger {}
    private val attrs: Map<String, String> by option(
        "-s",
        "--set",
        metavar = "ATTR=VALUE",
        help = "Set the attribute value in BaseDirectoryEntry."
    ).associate()
    private val deprecatedFile: String? by option(
        "--file",
        "-f",
        metavar = "FILENAME",
        help = "Read the BaseDirectoryEntry from specified file, use - to read data from STDIN"
    ).deprecated("WARINING: -f / --file ist deprecated. Use argument without option instead.")
    private val inputFile by argument(
        help = "Read the BaseDirectoryEntry from specified file, use - to read data from STDIN"
    ).path(mustExist = true, canBeDir = false, mustBeReadable = true).optional()
    private val context by requireObject<CommandContext>()
    private val ignore by option("--ignore", "-i", help = "Ignore Error 409 (entry exists).").flag()

    override fun run() = catching {
        val input = inputFile ?: deprecatedFile

        val data = if (input != null && input == "-") {
            logger.debug { "Loading from STDIN" }
            generateSequence(::readLine).joinToString("\n")
        } else if (input != null) {
            logger.debug { "Loading file: $input" }
            File(input.toString()).readText(Charsets.UTF_8)
        } else {
            null
        }

        val baseDirectoryEntry: BaseDirectoryEntry = data?.let {
            when (context.outputFormat) {
                OutputFormat.HUMAN, OutputFormat.YAML -> Yaml.decodeFromString(it)
                OutputFormat.JSON -> Json.decodeFromString(it)
                else -> throw CliktError("Unsupported format: ${context.outputFormat}")
            }
        } ?: run {
            val telematikID: String = attrs["telematikID"] ?: throw UsageError("Option --set telematikID=<VALUE> or --file is required")
            BaseDirectoryEntry(
                telematikID = telematikID,
                domainID = listOf("vzd-cli")
            )
        }

        setAttributes(baseDirectoryEntry, attrs)

        logger.debug { "Creating new directory entry with telematikID: ${baseDirectoryEntry.telematikID}" }

        val result = try {
            val dn = runBlocking { context.client.addDirectoryEntry(CreateDirectoryEntry(baseDirectoryEntry)) }
            logger.info("Created new DirectoryEntry: ${dn.uid}")

            val query = mapOf("uid" to dn.uid)
            runBlocking { context.client.readDirectoryEntry(query) }
        } catch (e: AdminResponseException) {
            if (!ignore || e.response.status != HttpStatusCode.Conflict) {
                throw e
            }
            logger.warn { "Entry with telematikID=${baseDirectoryEntry.telematikID} already exists. Ignoring conflict." }
            runBlocking { context.client.readDirectoryEntry(mapOf("telematikID" to baseDirectoryEntry.telematikID)) }
        }

        when (context.outputFormat) {
            OutputFormat.JSON -> Output.printJson(result?.first()?.directoryEntryBase)
            OutputFormat.HUMAN, OutputFormat.YAML -> Output.printYaml(result?.first()?.directoryEntryBase)
            else -> throw UsageError("Cant load for editing in for format: ${context.outputFormat}")
        }
    }
}
