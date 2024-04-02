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

class SearchHealthcareServiceCommand : CliktCommand(name = "healthcare-service", help = "Search HealthcareService resources (alias: hs)") {
    private val logger = KotlinLogging.logger {}
    private val context by requireObject<SearchCommand.SearchContext>()

    private val outputFormat by option().switch(
        "--json" to OutputFormat.JSON,
        "--json-ext" to OutputFormat.JSON_EXT,
        "--human" to OutputFormat.YAML_EXT,
    ).default(OutputFormat.YAML_EXT)

    private val active: Boolean by option("--active", "-a", help = "Filter by active status").flag(default = true)

    private val includeOrganization by option().switch(
        "--include-practitioner" to "HealthcareService:organization",
        "--exclude-practitioner" to "",
    ).default("HealthcareService:organization")

    private val includeLocation by option().switch(
        "--include-location" to "HealthcareService:location",
        "--exclude-location" to "",
    ).default("HealthcareService:location")

    private val includeEndpoint by option().switch(
        "--include-endpoint" to "PractitionerRole:endpoint",
        "--exclude-endpoint" to "",
    ).default("PractitionerRole:endpoint")

    private val telematikID by option("--telematik-id", "-t", help = "Telematik-ID of the Organization")

    override fun run() =
        catching {
            logger.info { "Searching HealthcareService resources in FHIR Directory ${context.ctx.env.name}" }
            val query = SearchQuery(SearchResource.HealthcareService)

            listOf(includeOrganization, includeLocation, includeEndpoint).forEach {
                if (it.isNotEmpty()) {
                    query.addParam("_include", it)
                }
            }

            query.addParam("organization.active", active.toString())

            if (telematikID != null) {
                query.addParam("organization.identifier", "https://gematik.de/fhir/sid/telematik-id|$telematikID")
            }

            runBlocking {
                val bundle = context.search(context.ctx, query)
                echo(bundle.toStringOutput(outputFormat))
            }
        }
}
