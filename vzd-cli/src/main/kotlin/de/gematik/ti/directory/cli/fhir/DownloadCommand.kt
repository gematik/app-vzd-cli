package de.gematik.ti.directory.cli.fhir

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.requireObject
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.types.path
import de.gematik.ti.directory.DirectoryException
import de.gematik.ti.directory.cli.catching
import de.gematik.ti.directory.fhir.Scope
import kotlinx.coroutines.runBlocking

class DownloadCommand : CliktCommand(name = "download", help = "Loads FHIR Bundle of a single practitioner or organisation") {
    private val context by requireObject<FhirCliEnvironmentContext>()

    private val telematikID by argument("TELEMATIK_ID", help = "Telematik-ID of the entry")

    private val outputFile by argument("OUTPUT_FILE", help = "Output file to write FHIR Bundle to").path(mustExist = false, canBeDir = false)

    override fun run() =
        catching {
            val bundle =
                runBlocking { context.client.findEntry(Scope.Owner, telematikID) }
                    ?: throw DirectoryException("No FHIR entry found for TelematikID $telematikID")
            echo("Written ${bundle.total} resource(s) to $outputFile")
            outputFile.toFile().writeText(bundle.toStringOutput(OutputFormat.JSON))
        }
}
