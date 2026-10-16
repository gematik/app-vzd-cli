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
import com.github.ajalt.clikt.core.UsageError
import com.github.ajalt.clikt.core.requireObject
import com.github.ajalt.clikt.output.TermUi
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.switch
import de.gematik.ti.directory.admin.BaseDirectoryEntry
import de.gematik.ti.directory.admin.UpdateBaseDirectoryEntry
import de.gematik.ti.directory.cli.catching
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import net.mamoe.yamlkt.Yaml

private val JsonPretty = Json { prettyPrint = true }

class EditCommand : CliktCommand(name = "edit", help = "Edit base entry using text editor") {
    private val telematikID by argument()
    private val context by requireObject<AdminCliEnvironmentContext>()
    private val format by option()
        .switch(
            "--json" to RepresentationFormat.JSON,
            "--yaml" to RepresentationFormat.YAML,
        ).default(RepresentationFormat.YAML)

    private val json = Json { ignoreUnknownKeys = true }

    override fun run() =
        catching {
            val queryResult = runBlocking { context.client.readDirectoryEntry(mapOf("telematikID" to telematikID)) }
            val entry = queryResult?.firstOrNull() ?: throw CliktError("Entry not found for TelematikID: $telematikID")

            val textToEdit =
                when (format) {
                    RepresentationFormat.YAML -> Yaml.encodeToString(entry.directoryEntryBase)
                    RepresentationFormat.JSON -> JsonPretty.encodeToString(entry.directoryEntryBase)
                    else -> throw UsageError("Unsupported edit format: $format")
                }

            TermUi.editText(textToEdit, requireSave = true)?.let { edited ->

                val editedBaseDirectoryEntry: BaseDirectoryEntry =
                    when (format) {
                        RepresentationFormat.YAML -> Yaml.decodeFromString(edited)
                        RepresentationFormat.JSON -> JsonPretty.decodeFromString(edited)
                        else -> throw UsageError("Unsupported edit format: $format")
                    }

                val uid = editedBaseDirectoryEntry.dn?.uid ?: throw CliktError("UID ist not specified")
                val jsonData = json.encodeToString(editedBaseDirectoryEntry)
                val updateBaseDirectoryEntry = json.decodeFromString<UpdateBaseDirectoryEntry>(jsonData)

                runBlocking { context.client.modifyDirectoryEntry(uid, updateBaseDirectoryEntry) }

                val result = runBlocking { context.client.readDirectoryEntry(mapOf("uid" to uid)) }

                val entryAfterEdit =
                    when (format) {
                        RepresentationFormat.YAML -> Yaml.encodeToString(result?.first()?.directoryEntryBase)
                        RepresentationFormat.JSON -> JsonPretty.encodeToString(result?.first()?.directoryEntryBase)
                        else -> throw UsageError("Unsupported edit format: $format")
                    }

                echo(entryAfterEdit)
            }
        }
}
