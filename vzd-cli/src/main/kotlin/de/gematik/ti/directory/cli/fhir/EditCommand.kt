/*
Copyright 2026, gematik GmbH

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

*******

For additional notes and disclaimer from gematik and in case of changes
by gematik, find details in the "Readme" file.
*/

package de.gematik.ti.directory.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.requireObject
import com.github.ajalt.clikt.output.TermUi
import com.github.ajalt.clikt.parameters.arguments.argument
import de.gematik.ti.directory.DirectoryException
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
            val bundle =
                runBlocking { context.client.findEntry(Scope.Owner, telematikID) }
                    ?: throw DirectoryException("No FHIR entry found for TelematikID $telematikID")
            val textToEdit = bundle.toStringOutput(OutputFormat.JSON)
            val editedText = TermUi.editText(textToEdit, requireSave = true)
            editedText?.let {
                uploadBundle(context, editedText)
            }
        }
}
