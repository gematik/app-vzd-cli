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
