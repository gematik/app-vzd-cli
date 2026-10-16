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
import com.github.ajalt.clikt.core.requireObject
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.multiple
import de.gematik.ti.directory.admin.DirectoryEntry
import de.gematik.ti.directory.admin.quickSearch
import de.gematik.ti.directory.cli.catching
import kotlinx.coroutines.runBlocking

class SearchCommand : CliktCommand(name = "search", help = "Search for directory entries") {
    private val arguments by argument().multiple()
    private val context by requireObject<AdminCliEnvironmentContext>()

    override fun run() =
        catching {
            val queryString = arguments.joinToString(" ")
            val result: List<DirectoryEntry> =
                runBlocking {
                    context.client.quickSearch(queryString).directoryEntries
                }
            echo(result.toTable())
        }
}
