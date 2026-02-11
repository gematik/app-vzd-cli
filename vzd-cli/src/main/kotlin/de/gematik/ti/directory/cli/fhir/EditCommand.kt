package de.gematik.ti.directory.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.requireObject
import com.github.ajalt.clikt.output.TermUi
import com.github.ajalt.clikt.parameters.arguments.argument
import de.gematik.ti.directory.cli.fhir.FhirCliEnvironmentContext
import de.gematik.ti.directory.cli.fhir.OutputFormat
import de.gematik.ti.directory.cli.fhir.toStringOutput
import de.gematik.ti.directory.cli.fhir.uploadBundle
import de.gematik.ti.directory.fhir.Scope
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging

class EditCommand : CliktCommand(name = "edit", help = "Edit resources of single organisation or practitioner") {
    private val logger = KotlinLogging.logger {}
    private val context by requireObject<FhirCliEnvironmentContext>()

    private val telematikID by argument("TELEMATIK_ID", help = "Telematik-ID of the entry")

    override fun run() =
        catching {
            val bundle = runBlocking { context.client.findEntry(Scope.Owner, telematikID) }
            val textToEdit = bundle.toStringOutput(OutputFormat.JSON)
            val editedText = TermUi.editText(textToEdit, requireSave = true)
            editedText?.let {
                uploadBundle(context, editedText)
            }
        }
}
