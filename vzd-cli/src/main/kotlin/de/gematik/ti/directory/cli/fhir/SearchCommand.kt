package de.gematik.ti.directory.cli.fhir

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.requireObject
import com.github.ajalt.clikt.core.subcommands
import de.gematik.ti.directory.cli.catching
import de.gematik.ti.directory.fhir.SearchQuery
import org.hl7.fhir.r4.model.Bundle

class SearchCommand(val search: suspend (FhirCliEnvironmentContext, SearchQuery) -> Bundle) : CliktCommand(
    name = "search",
    help = "Search FHIR Directory",
) {
    private val context by requireObject<FhirCliEnvironmentContext>()

    class SearchContext(val search: suspend (FhirCliEnvironmentContext, SearchQuery) -> Bundle, val ctx: FhirCliEnvironmentContext)

    init {
        subcommands(
            SearchHealthcareServiceCommand(),
            SearchPractitionerRoleCommand(),
        )
    }

    override fun aliases(): Map<String, List<String>> =
        mapOf(
            "hs" to listOf("healthcare-service"),
            "pr" to listOf("practitioner-role"),
        )

    override fun run() =
        catching {
            currentContext.obj = SearchContext(search, context)
        }
}
