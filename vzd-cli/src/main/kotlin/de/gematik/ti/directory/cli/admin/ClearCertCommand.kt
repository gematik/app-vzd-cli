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
import com.github.ajalt.clikt.core.UsageError
import com.github.ajalt.clikt.core.requireObject
import com.github.ajalt.clikt.parameters.groups.provideDelegate
import com.github.ajalt.clikt.parameters.options.associate
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.pair
import de.gematik.ti.directory.cli.catching
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import kotlin.io.path.Path
import kotlin.io.path.exists
import kotlin.io.path.useLines

class ClearCertCommand : CliktCommand(name = "clear-cert", help = "Clear all certificates of a given entry") {
    private val logger = KotlinLogging.logger {}
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
        metavar = "PARAM=VALUE",
    ).associate()
    private val parameterOptions by ParameterOptions()

    private val context by requireObject<AdminCliEnvironmentContext>()

    override fun run() =
        catching {
            val params = parameterOptions.toMap() + customParams
            paramFile?.let { paramFile ->
                val file = Path(paramFile.second)
                if (!file.exists()) throw CliktError("File not found: ${paramFile.second}")
                file.useLines { line ->
                    line.forEach {
                        runSingleCommand(params + Pair(paramFile.first, it))
                    }
                }
            } ?: run {
                runSingleCommand(params)
            }
        }

    private fun runSingleCommand(params: Map<String, String>) {
        if (params.isEmpty()) {
            throw UsageError("Specify at least one query parameter")
        }

        val result = runBlocking { context.client.readDirectoryEntry(params) }

        if (result.isNullOrEmpty()) {
            logger.info { "Entry not found: $params" }
            throw CliktError("Entry not found: $params")
        }

        // Make sure we clear only one Entry per run. Just to be safe.
        if (result.size > 1) {
            logger.error { "Query matches too many entries: $params" }
            throw CliktError("Query matches too many entries: $params")
        }

        val entry = result.first()

        logger.info { "Clearing ${entry.userCertificates?.size ?: 0} certificate(s) in ${entry.directoryEntryBase.telematikID}" }

        entry.userCertificates?.forEach {
            logger.debug { "Deleting certificate: ${it.userCertificate?.base64String}" }
            logger.debug { it.dn }
            echo("Deleting certificate: telematikID=${it.telematikID} serialNumber=${it.userCertificate?.certificateInfo?.serialNumber}")
            runBlocking { context.client.deleteDirectoryEntryCertificate(it.dn?.uid!!, it.dn?.cn!!) }
        }
    }
}
