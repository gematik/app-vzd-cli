package de.gematik.ti.directory.cli.fhir

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.requireObject
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.switch
import de.gematik.ti.directory.cli.catching
import kotlinx.coroutines.runBlocking

class ShowCommand : CliktCommand(name = "show", help = "SHows all Data of a single practitioner or organisation") {
    private val context by requireObject<FhirCliEnvironmentContext>()

    private val outputFormat by option()
        .switch(
            "--json" to OutputFormat.JSON,
            "--json-ext" to OutputFormat.JSON_EXT,
            "--yaml" to OutputFormat.YAML,
            "--human" to OutputFormat.HUMAN,
        ).default(OutputFormat.HUMAN)

    private val telematikID by argument("TELEMATIK_ID", help = "Telematik-ID of the entry")

    override fun run() =
        catching {
            val bundle = runBlocking { findEntry(context, telematikID) }
            echo(bundle.toStringOutput(outputFormat))
        }
}
