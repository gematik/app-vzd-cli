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
import de.gematik.ti.directory.DirectoryException
import de.gematik.ti.directory.cli.catching
import de.gematik.ti.directory.util.escape
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging

abstract class AbstractSwitchStateCommand(
    val active: Boolean,
    val activeLabel: String,
    name: String,
    help: String
) : CliktCommand(
        name = name,
        help = help,
    ) {
    private val logger = KotlinLogging.logger {}
    private val customParams: Map<String, String> by option(
        "-p",
        "--param",
        help = "Specify query parameters to find matching entries",
    ).associate()
    private val parameterOptions by ParameterOptions()

    private val context by requireObject<AdminCliEnvironmentContext>()

    override fun run() =
        catching {
            val params = parameterOptions.toMap() + customParams
            runBlocking {
                if (params.isEmpty()) {
                    throw UsageError("Specify at least one query parameter")
                }
                val result = context.client.readDirectoryEntry(params)
                if (result != null && result.size > 1) {
                    throw DirectoryException("Query must match only one entry (got ${result.size}")
                }
                result?.first()?.let {
                    logger.info { "Setting active=$active on entry ${it.directoryEntryBase.telematikID.escape()}" }
                    context.client.stateSwitch(it.directoryEntryBase.dn!!.uid, false)
                    val reloadedEntry = context.client.readDirectoryEntry(mapOf("uid" to it.directoryEntryBase.dn!!.uid))!!.first()
                    echo(
                        "Entry ${reloadedEntry.directoryEntryBase.telematikID.escape()} (${it.directoryEntryBase.displayName}) is $activeLabel",
                    )
                }
            }
        }
}

class DeactivateCommand : AbstractSwitchStateCommand(false, "deactivated", "deactivate", "Deactivates an entry (sets active=false)")

class ActivateCommand : AbstractSwitchStateCommand(true, "active", "activate", "Deactivates an entry (sets active=false)")
