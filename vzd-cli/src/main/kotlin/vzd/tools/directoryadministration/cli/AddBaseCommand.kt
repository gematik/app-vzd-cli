package vzd.tools.directoryadministration.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.CliktError
import com.github.ajalt.clikt.core.UsageError
import com.github.ajalt.clikt.core.requireObject
import com.github.ajalt.clikt.parameters.options.associate
import com.github.ajalt.clikt.parameters.options.option
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import mu.KotlinLogging
import net.mamoe.yamlkt.Yaml
import vzd.tools.directoryadministration.BaseDirectoryEntry
import vzd.tools.directoryadministration.CreateDirectoryEntry
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
        } else if (property.returnType == typeOf<List<String>>() || property.returnType == typeOf<List<String>?>()) {
            property.setter.call(baseDirectoryEntry, value.split(',').map { it.trim() })
        } else {
            throw UsageError("Unsupported property type '$name': ${property.returnType}")
        }
    }

}

class AddBaseCommand: CliktCommand(name="add-base", help="Add new directory entry") {
    private val logger = KotlinLogging.logger {}
    private val attrs: Map<String, String> by option("-s", "--set", metavar = "ATTR=VALUE",
        help="Set the attribute value in BaseDirectoryEntry.").associate()
    private val file: String? by option("--file", "-f", metavar = "FILENAME",
        help="Read the directory BaseDirectoryEntry from specified file, use - to read data from STDIN")
    private val context by requireObject<CommandContext>()

    override fun run() = catching {
        val data = if (file != null && file == "-") {
            logger.debug { "Loading from STDIN" }
            generateSequence(::readLine).joinToString("\n")
        } else if (file != null) {
            logger.debug { "Loading file: $file" }
            File(file.toString()).readText(Charsets.UTF_8)
        } else {
            null
        }

        val baseDirectoryEntry: BaseDirectoryEntry = data?.let {
            when(context.outputFormat) {
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

        val dn = runBlocking {  context.client.addDirectoryEntry(CreateDirectoryEntry(baseDirectoryEntry)) }

        logger.info("Created new DirectoryEntry: ${dn.uid}")

        val query = mapOf("uid" to dn.uid)
        val result = runBlocking {  context.client.readDirectoryEntry(query) }

        when (context.outputFormat) {
            OutputFormat.JSON -> Output.printJson(result?.first()?.directoryEntryBase)
            OutputFormat.HUMAN, OutputFormat.YAML -> Output.printYaml(result?.first()?.directoryEntryBase)
            else -> throw UsageError("Cant load for editing in for format: ${context.outputFormat}")
        }
    }
}

