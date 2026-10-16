/*
 * Copyright (Change Date see Readme), gematik GmbH
 *
 * Licensed under the EUPL, Version 1.2 or - as soon they will be approved by the
 * European Commission – subsequent versions of the EUPL (the "Licence").
 * You may not use this work except in compliance with the Licence.
 *
 * You find a copy of the Licence in the "Licence" file or at
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either expressed or implied.
 * In case of changes by gematik find details in the "Readme" file.
 *
 * See the Licence for the specific language governing permissions and limitations under the Licence.
 *
 * *******
 *
 * For additional notes and disclaimer from gematik and in case of changes by gematik,
 * find details in the "Readme" file.
 */

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
