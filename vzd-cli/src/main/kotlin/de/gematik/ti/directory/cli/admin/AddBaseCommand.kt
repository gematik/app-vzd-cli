package de.gematik.ti.directory.cli.admin

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.CliktError
import com.github.ajalt.clikt.core.UsageError
import com.github.ajalt.clikt.core.requireObject
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.optional
import com.github.ajalt.clikt.parameters.options.*
import com.github.ajalt.clikt.parameters.types.path
import de.gematik.ti.directory.admin.AdminResponseException
import de.gematik.ti.directory.admin.BaseDirectoryEntry
import de.gematik.ti.directory.admin.CreateDirectoryEntry
import de.gematik.ti.directory.cli.catching
import de.gematik.ti.directory.cli.toJsonPretty
import de.gematik.ti.directory.cli.toYaml
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import mu.KotlinLogging
import net.mamoe.yamlkt.Yaml
import java.beans.Introspector
import java.io.File

fun setAttributes(baseDirectoryEntry: BaseDirectoryEntry?, attrs: Map<String, String>) {
    attrs.forEach { (name, value) ->

        val beanInfo = Introspector.getBeanInfo(BaseDirectoryEntry::class.java)

        val property = beanInfo.propertyDescriptors.filter { it.writeMethod != null }.first { it.name == name }

        if (property.propertyType == String::class.java) {
            property.writeMethod.invoke(baseDirectoryEntry, value)
        } else if (property.propertyType == Int::class.java) {
            property.writeMethod.invoke(baseDirectoryEntry, value.toInt())
        } else if (property.readMethod.genericReturnType.typeName == "java.util.List<java.lang.String>") {
            property.writeMethod.invoke(baseDirectoryEntry, value.split(',').map { it.trim() })
        } else {
            throw UsageError("Unsupported property type '$name': ${property.readMethod.genericReturnType.typeName}")
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
    private val context by requireObject<AdminCliEnvironmentContext>()
    private val ignore by option("--ignore", "-i", help = "Ignore Error 409 (entry exists).").flag()
    private val format by option().switch(
        "--yaml" to RepresentationFormat.YAML,
        "--json" to RepresentationFormat.JSON
    ).default(RepresentationFormat.YAML)

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
            when (format) {
                RepresentationFormat.YAML -> Yaml.decodeFromString(it)
                RepresentationFormat.JSON -> Json.decodeFromString(it)
                else -> throw CliktError("Unsupported format: $format")
            }
        } ?: run {
            val telematikID: String = attrs["telematikID"] ?: throw UsageError("Option --set telematikID=<VALUE> or --file is required")
            BaseDirectoryEntry(
                telematikID = telematikID
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

        when (format) {
            RepresentationFormat.JSON -> echo(result?.first()?.directoryEntryBase?.toJsonPretty())
            RepresentationFormat.YAML -> echo(result?.first()?.directoryEntryBase?.toYaml())
            else -> throw UsageError("Cant load for editing in for format: $format")
        }
    }
}
