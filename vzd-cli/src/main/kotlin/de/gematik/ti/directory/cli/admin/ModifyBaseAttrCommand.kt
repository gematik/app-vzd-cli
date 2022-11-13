package de.gematik.ti.directory.cli.admin

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.CliktError
import com.github.ajalt.clikt.core.UsageError
import com.github.ajalt.clikt.core.requireObject
import com.github.ajalt.clikt.parameters.groups.provideDelegate
import com.github.ajalt.clikt.parameters.options.associate
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.switch
import de.gematik.ti.directory.admin.BaseDirectoryEntry
import de.gematik.ti.directory.admin.UpdateBaseDirectoryEntry
import de.gematik.ti.directory.cli.catching
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import mu.KotlinLogging

private val JSON = Json { ignoreUnknownKeys = true }

class ModifyBaseAttrCommand : CliktCommand(name = "modify-base-attr", help = "Modify specific attributes of a base entry") {
    private val logger = KotlinLogging.logger {}
    private val context by requireObject<AdminCliEnvironmentContext>()

    private val format by option().switch(
        "--yaml" to OutputFormat.YAML,
        "--json" to OutputFormat.JSON
    ).default(OutputFormat.YAML)
    private val customParams: Map<String, String> by option(
        "-p",
        "--param",
        help = "Specify query parameters to find matching entries"
    ).associate()
    private val parameterOptions by ParameterOptions()
    private val attrs: Map<String, String> by option(
        "-s",
        "--set",
        metavar = "ATTR=VALUE",
        help = "Set the attribute value in BaseDirectoryEntry."
    ).associate()

    override fun run() = catching {
        val params = parameterOptions.toMap() + customParams

        if (params.isEmpty()) {
            throw UsageError("Please specify at least one query parameter")
        }

        val baseToUpdate: BaseDirectoryEntry? = params.let {
            val result = runBlocking { context.client.readDirectoryEntry(params) }
            if ((result?.size ?: 0) > 1) {
                throw CliktError("Found too many entries: ${result?.size}. Please change your query.")
            }
            result?.first()?.directoryEntryBase
        }

        val dn = baseToUpdate?.dn

        setAttributes(baseToUpdate, attrs)

        logger.debug { "Data will to send to server: $baseToUpdate" }

        if (dn != null) {
            val jsonData = Json.encodeToString(baseToUpdate)
            val updateBaseDirectoryEntry =
                JSON.decodeFromString<UpdateBaseDirectoryEntry>(jsonData)
            // server bug: when updating telematikID with no certificates the exception is thrown
            updateBaseDirectoryEntry.telematikID = null
            runBlocking { context.client.modifyDirectoryEntry(dn.uid, updateBaseDirectoryEntry) }
            val result = runBlocking { context.client.readDirectoryEntry(mapOf("uid" to dn.uid)) }

            when (format) {
                OutputFormat.JSON -> Output.printJson(result?.first()?.directoryEntryBase)
                OutputFormat.YAML -> Output.printYaml(result?.first()?.directoryEntryBase)
                else -> throw UsageError("Unsupported format: $format")
            }
        }
    }
}
