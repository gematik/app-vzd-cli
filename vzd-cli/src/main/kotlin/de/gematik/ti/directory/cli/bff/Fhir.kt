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

package de.gematik.ti.directory.cli.bff

import de.gematik.ti.directory.DirectoryEnvironment
import de.gematik.ti.directory.fhir.FhirContextR4
import de.gematik.ti.directory.fhir.Scope
import io.ktor.http.*
import io.ktor.resources.*
import io.ktor.server.application.*
import io.ktor.server.resources.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable

@Serializable
@Resource("fhir")
class Fhir {
    @Serializable
    @Resource("{envTitle}")
    class Env(
        val parent: Fhir = Fhir(),
        private val envTitle: String
    ) {
        val env get() = DirectoryEnvironment.valueOf(envTitle)

        @Serializable
        @Resource("search")
        class Search(
            val parent: Env,
            val q: String
        )

        @Resource("entry/{telematikID}")
        @Serializable
        data class Entry(
            val parent: Env,
            val telematikID: String
        )
    }
}

fun Route.fhirRoutes() {
    get<Fhir.Env.Search> { _ ->
    }

    get<Fhir.Env.Entry> { entry ->
        val fhirApi = application.attributes[FhirAPIAttributeName]
        val fhirClient = fhirApi.createClient(entry.parent.env)
        val fhirParser = FhirContextR4.newJsonParser()

        try {
            val bundle =
                fhirClient.findEntry(Scope.Owner, entry.telematikID)
                    ?: fhirClient.findEntry(Scope.Search, entry.telematikID)

            if (bundle == null) {
                call.respond(
                    HttpStatusCode.NotFound,
                    Outcome(
                        code = "not_found",
                        message = "Entry ${entry.telematikID} not found",
                    ),
                )
                return@get
            }

            call.response.header(HttpHeaders.CacheControl, "no-store")
            call.respondText(fhirParser.encodeResourceToString(bundle), ContentType("application", "fhir+json"))
        } catch (e: Exception) {
            logger.error(e) { "Failed to find entry ${entry.telematikID}" }
            call.respond(
                HttpStatusCode.InternalServerError,
                Outcome(
                    code = "internal_server_error",
                    message = e.message ?: "An unexpected error occurred",
                ),
            )
        }
    }
}
