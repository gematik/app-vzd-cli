package de.gematik.ti.directory.cli.fhir

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.requireObject
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.switch
import de.gematik.ti.directory.cli.catching
import de.gematik.ti.directory.fhir.Scope
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

    private val scope by option(help = "FHIR resource scope")
        .switch(
            "--search" to Scope.Search,
            "--owner" to Scope.Owner,
            "--fdv" to Scope.Fdv,
        )

    private val telematikID by argument("TELEMATIK_ID", help = "Telematik-ID of an entry to show")

    override fun run() =
        catching {
            val bundle =
                runBlocking {
                    val selectedScope = scope
                    if (selectedScope != null) {
                        context.client.findEntry(selectedScope, telematikID)
                    } else {
                        try {
                            context.client.findEntry(Scope.Owner, telematikID)
                        } catch (_: Exception) {
                            context.client.findEntry(Scope.Search, telematikID)
                        }
                    }
                }
            echo(bundle.toStringOutput(outputFormat))
        }
}
