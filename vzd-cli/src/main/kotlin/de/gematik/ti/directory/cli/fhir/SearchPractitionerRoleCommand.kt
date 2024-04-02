package de.gematik.ti.directory.cli.fhir

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.requireObject
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.switch
import de.gematik.ti.directory.cli.catching
import de.gematik.ti.directory.fhir.SearchQuery
import de.gematik.ti.directory.fhir.SearchResource
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging

class SearchPractitionerRoleCommand: CliktCommand(name = "practitioner-role", help = "Search PractitionerRole resources (alias: pr)") {
    private val logger = KotlinLogging.logger {}
    private val context by requireObject<FhirCliEnvironmentContext>()

    private val outputFormat by option().switch(
        "--json" to OutputFormat.JSON,
        "--json-ext" to OutputFormat.JSON_EXT,
        "--human" to OutputFormat.YAML_EXT,
    ).default(OutputFormat.YAML_EXT)

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

    private val telematikID by option("--telematik-id", "-t", help = "Telematik-ID of the Practitioner")

    override fun run() =
        catching {
            logger.info { "Searching PractitionerRole resources in FHIR Directory ${context.env.name}" }
            val query = SearchQuery(SearchResource.PractitionerRole)

            listOf(includePractitioner, includeLocation, includeEndpoint).forEach {
                if (it.isNotEmpty()) {
                    query.addParam("_include", it)
                }
            }

            query.addParam("practitioner.active", active.toString())

            if (telematikID != null) {
                query.addParam("practitioner.identifier", "https://gematik.de/fhir/sid/telematik-id|$telematikID")
            }

            runBlocking {
                val bundle = context.client.search(query)
                echo(bundle.toStringOutput(outputFormat))
            }
        }
}