/*
Copyright 2026, gematik GmbH

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

*******

For additional notes and disclaimer from gematik and in case of changes
by gematik, find details in the "Readme" file.
*/

package de.gematik.ti.directory.cli.fhir

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.requireObject
import com.github.ajalt.clikt.core.subcommands
import de.gematik.ti.directory.cli.catching
import de.gematik.ti.directory.fhir.SearchQuery
import org.hl7.fhir.r4.model.Bundle

class SearchCommand(
    val search: suspend (FhirCliEnvironmentContext, SearchQuery) -> Bundle
) : CliktCommand(
        name = "search",
        help = "Search FHIR Directory",
    ) {
    private val context by requireObject<FhirCliEnvironmentContext>()

    class SearchContext(
        val search: suspend (FhirCliEnvironmentContext, SearchQuery) -> Bundle,
        val ctx: FhirCliEnvironmentContext
    )

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
