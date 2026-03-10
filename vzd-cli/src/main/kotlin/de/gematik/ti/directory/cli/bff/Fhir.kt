package de.gematik.ti.directory.cli.bff

import de.gematik.ti.directory.DirectoryEnvironment
import de.gematik.ti.directory.fhir.Scope
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
    get<Fhir.Env.Search> { search ->
        val fhirApi = application.attributes[FhirAPIAttributeName]
        val fhirClient = fhirApi.createClient(search.parent.env)
    }

    get<Fhir.Env.Entry> { entry ->
        val fhirApi = application.attributes[FhirAPIAttributeName]
        val fhirClient = fhirApi.createClient(entry.parent.env)

        val bundle =
            try {
                fhirClient.findEntry(Scope.Search, entry.telematikID)
            } catch (e: Exception) {
                logger.warn(e) { "Entry with id ${entry.telematikID} not found in public search (${entry.parent.env}). Trying owner." }
                fhirClient.findEntry(Scope.Owner, entry.telematikID)
            }

        call.respond()
    }
}
