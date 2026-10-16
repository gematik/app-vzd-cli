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
