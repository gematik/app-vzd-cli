package de.gematik.ti.directory.cli.fhir

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.requireObject
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.multiple
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.switch
import de.gematik.ti.directory.cli.catching
import de.gematik.ti.directory.fhir.SearchQuery
import de.gematik.ti.directory.fhir.SearchResource
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging

class SearchPractitionerRoleCommand : CliktCommand(name = "practitioner-role", help = "Search PractitionerRole resources (alias: pr)") {
    private val logger = KotlinLogging.logger {}
    private val context by requireObject<SearchCommand.SearchContext>()

    private val outputFormat by option().switch(
        "--json" to OutputFormat.JSON,
        "--json-ext" to OutputFormat.JSON_EXT,
        "--human" to OutputFormat.YAML_EXT,
        "--table" to OutputFormat.TABLE,
    ).default(OutputFormat.TABLE)

    private val active: Boolean by option("--active", "-a", help = "Filter by active status").flag(default = true)

    private val includePractitioner by option().switch(
        "--include-practitioner" to "PractitionerRole:practitioner",
        "--exclude-practitioner" to "",
    ).default("PractitionerRole:practitioner")

    private val includeLocation by option().switch(
        "--include-location" to "PractitionerRole:location",
        "--exclude-location" to "",
    ).default("PractitionerRole:location")

    private val includeEndpoint by option().switch(
        "--include-endpoint" to "PractitionerRole:endpoint",
        "--exclude-endpoint" to "",
    ).default("PractitionerRole:endpoint")

    private val textArguments by argument("SEARCH_TEXT").multiple(required = false)

    private val telematikID by option("--telematik-id", "-t", help = "Telematik-ID of the Practitioner")

    override fun run() =
        catching {
            logger.info { "Searching PractitionerRole resources in FHIR Directory ${context.ctx.env.name}" }
            val query = SearchQuery(SearchResource.PractitionerRole)

            listOf(includePractitioner, includeLocation, includeEndpoint).forEach {
                if (it.isNotEmpty()) {
                    query.addParam("_include", it)
                }
            }

            query.addParam("practitioner.active", active.toString())

            if (textArguments.isNotEmpty()) {
                query.addParam("_text", textArguments.joinToString(" "))
            }

            if (telematikID != null) {
                query.addParam("practitioner.identifier", "https://gematik.de/fhir/sid/telematik-id|$telematikID")
            }

            runBlocking {
                val bundle = context.search(context.ctx, query)
                echo(bundle.toStringOutput(outputFormat))
            }
        }
}
