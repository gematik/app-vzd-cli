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
