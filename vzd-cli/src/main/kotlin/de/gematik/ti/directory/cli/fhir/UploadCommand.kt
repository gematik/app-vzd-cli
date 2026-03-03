package de.gematik.ti.directory.cli.fhir

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.requireObject
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.switch
import com.github.ajalt.clikt.parameters.types.path
import de.gematik.ti.directory.DirectoryException
import de.gematik.ti.directory.cli.catching
import de.gematik.ti.directory.fhir.FHIR_R4
import kotlinx.coroutines.runBlocking
import org.hl7.fhir.r4.model.Bundle
import kotlin.io.path.readText

class UploadCommand : CliktCommand(name = "upload", help = "Uploads the content of FHIR Bundle") {
    private val context by requireObject<FhirCliEnvironmentContext>()

    private val outputFormat by option()
        .switch(
            "--json" to OutputFormat.JSON,
            "--json-ext" to OutputFormat.JSON_EXT,
            "--yaml" to OutputFormat.YAML,
            "--human" to OutputFormat.HUMAN,
        ).default(OutputFormat.JSON)

    private val inputFile by argument("INPUT_FILE", help = "Input file with FHIR Bundle").path(mustBeReadable = true, canBeDir = false)

    override fun run() =
        catching {
            uploadBundle(context, inputFile.readText())
        }
}

fun uploadBundle(
    context: FhirCliEnvironmentContext,
    bundleText: String
) {
    val parser = FHIR_R4.newJsonParser()
    val bundle =
        try {
            parser.parseResource(Bundle::class.java, bundleText)
        } catch (e: Exception) {
            logger.warn("Error parsing bundle", e)
            throw DirectoryException("Error parsing bundle: ${e.message}")
        }
    bundle.entry.forEach { entry ->
        try {
            runBlocking { context.client.put(entry.resource) }
            println("Saved ${entry.resource.resourceType.name}/${entry.resource.idElement.idPart}")
        } catch (e: Exception) {
            logger.warn("Error saving ${entry.resource.resourceType.name}/${entry.resource.idElement.idPart}", e)
            System.err.println("Error saving ${entry.resource.resourceType.name}/${entry.resource.idElement.idPart}: ${e.message}")
        }
    }
}
