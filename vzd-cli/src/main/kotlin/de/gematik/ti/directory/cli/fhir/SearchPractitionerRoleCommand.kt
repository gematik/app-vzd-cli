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
import com.github.ajalt.clikt.parameters.arguments.multiple
import com.github.ajalt.clikt.parameters.options.*
import de.gematik.ti.directory.cli.catching
import de.gematik.ti.directory.fhir.SearchQuery
import de.gematik.ti.directory.fhir.SearchResource
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging

class SearchPractitionerRoleCommand : CliktCommand(name = "practitioner-role", help = "Search PractitionerRole resources (alias: pr)") {
    private val logger = KotlinLogging.logger {}
    private val context by requireObject<SearchCommand.SearchContext>()

    private val outputFormat by option()
        .switch(
            "--json" to OutputFormat.JSON,
            "--json-ext" to OutputFormat.JSON_EXT,
            "--yaml" to OutputFormat.YAML,
            "--human" to OutputFormat.HUMAN,
            "--table" to OutputFormat.TABLE,
        ).default(OutputFormat.TABLE)

    private val active: Boolean by option("--active", "-a", help = "Filter by active status").flag(default = true)

    private val includePractitioner by option()
        .switch(
            "--include-practitioner" to "PractitionerRole:practitioner",
            "--exclude-practitioner" to "",
        ).default("PractitionerRole:practitioner")

    private val includeLocation by option()
        .switch(
            "--include-location" to "PractitionerRole:location",
            "--exclude-location" to "",
        ).default("PractitionerRole:location")

    private val includeEndpoint by option()
        .switch(
            "--include-endpoint" to "PractitionerRole:endpoint",
            "--exclude-endpoint" to "",
        ).default("PractitionerRole:endpoint")

    private val textArguments by argument("SEARCH_TEXT").multiple(required = false)
    private val telematikID by option("--telematik-id", "-t", help = "Telematik-ID of the Practitioner")
    private val professionOID by option("--professionOID", help = "OID of the profession")
    private val summary by option("--summary", "-s", help = "Summary mode").flag()

    private val customParams: Map<String, String> by option(
        "-p",
        "--param",
        help = "Specify query parameters to find matching entries",
        metavar = "NAME=VALUE",
    ).associate()

    override fun run() =
        catching {
            logger.info { "Searching PractitionerRole resources in FHIR Directory ${context.ctx.env.name}" }
            val query = SearchQuery(SearchResource.PractitionerRole)

            listOf(includePractitioner, includeLocation, includeEndpoint).forEach {
                if (it.isNotEmpty()) {
                    query.addParam("_include", it)
                }
            }
            customParams.forEach {
                query.addParam(it.key, it.value)
            }

            query.addParam("practitioner.active", active.toString())

            if (textArguments.isNotEmpty()) {
                query.addParam("_text", textArguments.joinToString(" "))
            }

            if (telematikID != null) {
                query.addParam("practitioner.identifier", "https://gematik.de/fhir/sid/telematik-id|$telematikID")
            }

            if (professionOID != null) {
                query.addParam(
                    "practitioner.qualification",
                    "https://gematik.de/fhir/directory/CodeSystem/PractitionerProfessionOID|$professionOID",
                )
            }

            if (summary) {
                query.addParam("_summary", "count")
            }

            customParams.forEach {
                query.addParam(it.key, it.value)
            }

            val bundle =
                runBlocking {
                    context.search(context.ctx, query)
                }

            if (summary) {
                echo("Total: ${bundle.total}")
            } else {
                echo(bundle.toStringOutput(outputFormat))
            }
        }
}
