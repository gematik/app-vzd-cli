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
import com.github.ajalt.clikt.parameters.groups.provideDelegate
import com.github.ajalt.clikt.parameters.options.*
import com.github.ajalt.clikt.parameters.types.path
import de.gematik.ti.directory.admin.*
import de.gematik.ti.directory.cli.OcspOptions
import de.gematik.ti.directory.cli.catching
import kotlinx.coroutines.runBlocking
import java.io.PrintStream
import kotlin.io.path.Path
import kotlin.io.path.exists
import kotlin.io.path.outputStream
import kotlin.io.path.useLines

class ListCommand : CliktCommand(name = "list", help = "List directory entries") {
    private val context by requireObject<AdminCliEnvironmentContext>()
    private val outputFormat by option()
        .switch(
            "--human" to RepresentationFormat.HUMAN,
            "--json" to RepresentationFormat.JSON,
            "--yaml" to RepresentationFormat.YAML,
            "--csv" to RepresentationFormat.CSV,
            "--table" to RepresentationFormat.TABLE,
            "--yaml-ext" to RepresentationFormat.YAML_EXT,
            "--json-ext" to RepresentationFormat.JSON_EXT,
        ).default(RepresentationFormat.HUMAN)

    private val paramFile: Pair<String, String>? by option(
        "-f",
        "--param-file",
        help = "Read parameter values from file",
        metavar = "PARAM FILENAME",
    ).pair()
    private val customParams: Map<String, String> by option(
        "-p",
        "--param",
        help = "Specify query parameters to find matching entries",
        metavar = "NAME=VALUE",
    ).associate()
    private val outfile by option(
        "-o",
        "--outfile",
        help = "Write output to file",
    ).path(mustExist = false, canBeDir = false, canBeFile = true)
    private val parameterOptions by ParameterOptions()
    private val sync by option(help = "use Sync mode").flag()
    private val ocspOptions by OcspOptions()

    private val kimParameterOptions by KimParameterOptions()

    override fun run() =
        catching {
            val plainParams = parameterOptions.toMap() + customParams
            val kimParams = kimParameterOptions.toMap()

            // cant use both KIM and Query parameters
            if (kimParams.isNotEmpty() && plainParams.isNotEmpty()) {
                throw CliktError("Cannot use both plain and KIM query parameters")
            }

            val query: Triple<Map<String, String>, String, String> =
                if (kimParams.isNotEmpty()) {
                    Triple(
                        kimParams,
                        ResourceDirectoryEntriesByKim,
                        ResourceDirectoryEntriesSyncByKim,
                    )
                } else {
                    Triple(
                        plainParams,
                        ResourceDirectoryEntries,
                        ResourceDirectoryEntriesSync,
                    )
                }

            val entries =
                buildList {
                    paramFile?.let { paramFile ->
                        val file = Path(paramFile.second)
                        if (!file.exists()) throw CliktError("File not found: ${paramFile.second}")
                        file.useLines { line ->
                            line.forEach {
                                runQuery(query.first + Pair(paramFile.first, it), query.second, query.third)?.let { addAll(it) }
                            }
                        }
                    } ?: run {
                        runQuery(query.first, query.second, query.third)?.let { addAll(it) }
                    }
                }

            val stdout = System.`out`
            try {
                outfile?.let { System.setOut(PrintStream(it.outputStream())) }
                echo(entries.toStringRepresentation(outputFormat))
            } finally {
                System.setOut(stdout)
            }
        }

    private fun runQuery(
        params: Map<String, String>,
        resource: String,
        resourceSync: String
    ): List<DirectoryEntry>? {
        val result: List<DirectoryEntry>? =
            if (sync) {
                runBlocking {
                    buildList {
                        context.client.streamDirectoryEntriesPaging(params, resource = resourceSync) {
                            add(it)
                        }
                    }
                }
            } else {
                runBlocking { context.client.readDirectoryEntry(params, resource = resource) }
            }

        if (ocspOptions.enableOcsp) {
            runBlocking { context.adminAPI.expandOCSPStatus(result) }
        }

        return result
    }
}
