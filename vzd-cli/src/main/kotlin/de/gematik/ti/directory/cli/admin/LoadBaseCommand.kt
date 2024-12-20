package de.gematik.ti.directory.cli.admin

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.UsageError
import com.github.ajalt.clikt.core.requireObject
import com.github.ajalt.clikt.parameters.groups.provideDelegate
import com.github.ajalt.clikt.parameters.options.associate
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.switch
import de.gematik.ti.directory.cli.catching
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import net.mamoe.yamlkt.Yaml

class LoadBaseCommand : CliktCommand(name = "load-base", help = "Load the base entry for editing.") {
    private val customParams: Map<String, String> by option(
        "-p",
        "--param",
        help = "Specify query parameters to find matching entries",
    ).associate()
    private val parameterOptions by ParameterOptions()
    private val context by requireObject<AdminCliEnvironmentContext>()
    private val format by option()
        .switch(
            "--json" to RepresentationFormat.JSON,
            "--yaml" to RepresentationFormat.YAML,
        ).default(RepresentationFormat.YAML)

    private val json =
        Json {
            prettyPrint = true
            encodeDefaults = true
        }

    private val yaml =
        Yaml {
            encodeDefaultValues = true
        }

    override fun run() =
        catching {
            val params = parameterOptions.toMap() + customParams
            val result =
                runBlocking {
                    context.client.readDirectoryEntry(params)
                }

            if (result?.size != 1) {
                throw UsageError("The query must return exactly one value. Got: ${result?.size}")
            }

            when (format) {
                RepresentationFormat.JSON -> println(json.encodeToString(result.first().directoryEntryBase))
                RepresentationFormat.HUMAN, RepresentationFormat.YAML -> println(yaml.encodeToString(result.first().directoryEntryBase))
                else -> throw UsageError("Unable to load for editing in for format: $format")
            }
        }
}
