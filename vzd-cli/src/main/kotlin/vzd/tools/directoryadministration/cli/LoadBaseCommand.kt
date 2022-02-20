package vzd.tools.directoryadministration.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.UsageError
import com.github.ajalt.clikt.core.requireObject
import com.github.ajalt.clikt.parameters.options.associate
import com.github.ajalt.clikt.parameters.options.option
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import net.mamoe.yamlkt.Yaml

class LoadBaseCommand: CliktCommand(name = "load-base", help="Load the base entry for editing.") {
    private val params: Map<String, String> by option("-p", "--param",
        help="Specify query parameters to find matching entries").associate()
    private val context by requireObject<CommandContext>()

    private val json = Json {
        prettyPrint = true
        encodeDefaults = true
    }

    private val yaml = Yaml {
        encodeDefaultValues = true
    }

    override fun run() = catching {
        val result = runBlocking {
            context.client.readDirectoryEntry(params)
        }

        if (result?.size != 1) {
            throw UsageError("The query must return exactly one value. Got: ${result?.size}")
        }

        when (context.outputFormat) {
            OutputFormat.JSON -> println ( json.encodeToString(result.first().directoryEntryBase) )
            OutputFormat.HUMAN, OutputFormat.YAML -> println ( yaml.encodeToString(result.first().directoryEntryBase) )
            else -> throw UsageError("Cant load for editing in for format: ${context.outputFormat}")
        }
    }

}