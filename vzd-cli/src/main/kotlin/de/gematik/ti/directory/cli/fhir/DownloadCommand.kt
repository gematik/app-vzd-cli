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
