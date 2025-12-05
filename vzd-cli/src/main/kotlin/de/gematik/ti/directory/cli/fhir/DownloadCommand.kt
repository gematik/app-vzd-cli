package de.gematik.ti.directory.cli.fhir

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.requireObject
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.types.path
import de.gematik.ti.directory.DirectoryException
import de.gematik.ti.directory.cli.catching
import de.gematik.ti.directory.fhir.SearchQuery
import de.gematik.ti.directory.fhir.SearchResource
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.hl7.fhir.r4.model.Bundle

class DownloadCommand : CliktCommand(name = "download", help = "Loads FHIR Bundle of a single practitioner or organisation") {
    private val context by requireObject<FhirCliEnvironmentContext>()

    private val telematikID by argument("TELEMATIK_ID", help = "Telematik-ID of the entry")

    private val outputFile by argument("OUTPUT_FILE", help = "Output file to write FHIR Bundle to").path(mustExist = false, canBeDir = false)

    override fun run() =
        catching {
            val bundle = runBlocking { findEntry(context, telematikID) }
            echo("Written ${bundle.total} resource(s) to $outputFile")
            outputFile.toFile().writeText(bundle.toStringOutput(OutputFormat.JSON))
        }
}

fun findEntry(
    context: FhirCliEnvironmentContext,
    telematikID: String
): Bundle {
    var practitionerBundle: Bundle? = null
    var healthcareServiceBundle: Bundle? = null

    runBlocking {
        val practitionerJob =
            launch {
                val practitionerRoleQuery = SearchQuery(SearchResource.PractitionerRole)
                practitionerRoleQuery.addParam(
                    "practitioner.identifier",
                    "https://gematik.de/fhir/sid/telematik-id|$telematikID",
                )
                practitionerRoleQuery.addParam("_include", "PractitionerRole:practitioner")
                practitionerRoleQuery.addParam("_include", "PractitionerRole:location")
                practitionerRoleQuery.addParam("_include", "PractitionerRole:endpoint")
                practitionerBundle = context.client.searchOwner(practitionerRoleQuery)
            }
        val healthcareServiceJob =
            launch {
                val healthcareServiceQuery = SearchQuery(SearchResource.HealthcareService)
                healthcareServiceQuery.addParam(
                    "organization.identifier",
                    "https://gematik.de/fhir/sid/telematik-id|$telematikID",
                )
                healthcareServiceQuery.addParam("_include", "HealthcareService:organization")
                healthcareServiceQuery.addParam("_include", "HealthcareService:location")
                healthcareServiceQuery.addParam("_include", "HealthcareService:endpoint")
                healthcareServiceBundle = context.client.searchOwner(healthcareServiceQuery)
            }

        practitionerJob.join()
        healthcareServiceJob.join()
    }
    val bundle =
        practitionerBundle?.takeIf { it.total > 0 }
            ?: healthcareServiceBundle?.takeIf { it.total > 0 }
            ?: throw DirectoryException("Entry with TelematikID `$telematikID` not found.")
    return bundle
}
