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
