package de.gematik.ti.directory.cli.fhir

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.CliktError
import com.github.ajalt.clikt.core.requireObject
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.switch
import de.gematik.ti.directory.cli.catching
import de.gematik.ti.directory.fhir.SearchQuery
import de.gematik.ti.directory.fhir.SearchResource
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging

class FdvShowCommand : CliktCommand(name = "show", help = "Shows an entry") {
    private val logger = KotlinLogging.logger {}
    private val context by requireObject<FhirCliEnvironmentContext>()

    private val telematikID by argument("TELEMATIK_ID", help = "Telematik-ID of the entry")

    private val outputFormat by option().switch(
        "--json" to OutputFormat.JSON,
        "--json-ext" to OutputFormat.JSON_EXT,
        "--human" to OutputFormat.YAML_EXT,
    ).default(OutputFormat.YAML_EXT)

    private val active: Boolean by option("--active", "-a", help = "Filter by active status").flag(default = true)

    override fun run() =
        catching {
                val channel = Channel<Boolean>()
                runBlocking {
                    launch {
                        try {
                            val practitionerRoleQuery = SearchQuery(SearchResource.PractitionerRole)
                            practitionerRoleQuery.addParam("practitioner.active", active.toString())
                            practitionerRoleQuery.addParam("practitioner.identifier", "https://gematik.de/fhir/sid/telematik-id|$telematikID")
                            practitionerRoleQuery.addParam("_include", "PractitionerRole:practitioner")
                            practitionerRoleQuery.addParam("_include", "PractitionerRole:location")
                            practitionerRoleQuery.addParam("_include", "PractitionerRole:endpoint")
                            val practitionerRoleBundle = context.client.searchFdv(practitionerRoleQuery)
                            if (practitionerRoleBundle.entry.isNotEmpty()) {
                                echo(practitionerRoleBundle.toStringOutput(outputFormat))
                                channel.send(true)
                            } else {
                                channel.send(false)
                            }
                        } catch (e: Exception) {
                            logger.info(e) { "No PractitionerRole found for Telematik-ID $telematikID" }
                            channel.send(false)
                        }
                    }
                    launch {
                        try {
                            val healthcareServiceQuery = SearchQuery(SearchResource.HealthcareService)
                            healthcareServiceQuery.addParam("organization.active", active.toString())
                            healthcareServiceQuery.addParam("organization.identifier", "https://gematik.de/fhir/sid/telematik-id|$telematikID")
                            healthcareServiceQuery.addParam("_include", "HealthcareService:organization")
                            healthcareServiceQuery.addParam("_include", "HealthcareService:location")
                            healthcareServiceQuery.addParam("_include", "HealthcareService:endpoint")
                            val healthcareServiceBundle = context.client.searchFdv(healthcareServiceQuery)
                            if (healthcareServiceBundle.entry.isNotEmpty()) {
                                echo(healthcareServiceBundle.toStringOutput(outputFormat))
                                channel.send(true)
                            } else {
                                channel.send(false)
                            }
                        } catch (e: Exception) {
                            logger.info(e) { "No HealthcareService found for Telematik-ID $telematikID" }
                            channel.send(false)
                        }
                    }

                    if (!channel.receive() && !channel.receive()) {
                        throw CliktError("No entry found for Telematik-ID $telematikID")
                    }


            }
        }
}