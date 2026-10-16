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

package de.gematik.ti.directory.cli.admin

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.CliktError
import com.github.ajalt.clikt.core.requireObject
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.groups.provideDelegate
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.switch
import de.gematik.ti.directory.admin.readDirectoryEntryByTelematikID
import de.gematik.ti.directory.cli.OcspOptions
import de.gematik.ti.directory.cli.catching
import kotlinx.coroutines.runBlocking

class ShowCommand : CliktCommand(name = "show", help = "Show all information about an entry") {
    private val outputFormat by option()
        .switch(
            "--human" to RepresentationFormat.HUMAN,
            "--json" to RepresentationFormat.JSON,
            "--yaml" to RepresentationFormat.YAML,
            "--yaml-ext" to RepresentationFormat.YAML_EXT,
            "--json-ext" to RepresentationFormat.JSON_EXT,
        ).default(RepresentationFormat.HUMAN)
    private val ocspOptions by OcspOptions()
    private val context by requireObject<AdminCliEnvironmentContext>()
    private val telematikID by argument("TELEMATIK_ID", help = "Telematik-ID of an entry to show")

    override fun run() =
        catching {
            val client = context.client
            val entry =
                runBlocking { client.readDirectoryEntryByTelematikID(telematikID) }
                    ?: throw CliktError("Entry with TelematikID '$telematikID' not found")

            if (ocspOptions.enableOcsp) {
                runBlocking { context.adminAPI.expandOCSPStatus(listOf(entry)) }
            }

            echo(entry.toStringRepresentation(outputFormat))
        }
}
