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

package de.gematik.ti.directory.cli.fhir

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.requireObject
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.switch
import de.gematik.ti.directory.DirectoryException
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
                } ?: throw DirectoryException("No FHIR entry found for TelematikID $telematikID")
            echo(bundle.toStringOutput(outputFormat))
        }
}
