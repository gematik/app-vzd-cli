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
import com.github.ajalt.clikt.core.UsageError
import com.github.ajalt.clikt.core.requireObject
import com.github.ajalt.clikt.parameters.groups.provideDelegate
import com.github.ajalt.clikt.parameters.options.associate
import com.github.ajalt.clikt.parameters.options.option
import de.gematik.ti.directory.cli.catching
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging

class DeleteCommand : CliktCommand(name = "delete", help = "Delete specified directory entries") {
    private val logger = KotlinLogging.logger {}
    private val customParams: Map<String, String> by option(
        "-p",
        "--param",
        help = "Specify query parameters to find matching entries",
    ).associate()
    private val parameterOptions by ParameterOptions()

    // val force by option(help="Force delete").flag()
    private val context by requireObject<AdminCliEnvironmentContext>()

    override fun run() =
        catching {
            val params = parameterOptions.toMap() + customParams
            runBlocking {
                if (params.isEmpty()) {
                    throw UsageError("Specify at least one query parameter")
                }
                val result = context.client.readDirectoryEntry(params)
                result?.forEach {
                    val answer =
                        prompt("Type YES to delete '${it.directoryEntryBase.telematikID}' '${it.directoryEntryBase.displayName}': ")
                    if (answer == "YES") {
                        logger.debug { "Deleting '${it.directoryEntryBase.displayName}' '${it.directoryEntryBase.dn?.uid}'" }
                        if (it.directoryEntryBase.dn?.uid != null) {
                            context.client.deleteDirectoryEntry(it.directoryEntryBase.dn!!.uid)
                        }
                    }
                }
            }
        }
}
