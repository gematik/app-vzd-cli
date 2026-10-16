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
