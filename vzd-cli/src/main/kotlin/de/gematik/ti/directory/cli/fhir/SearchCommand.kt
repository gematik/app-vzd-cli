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
