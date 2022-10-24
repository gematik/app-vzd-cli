package de.gematik.ti.directory.bff

import de.gematik.ti.directory.admin.AdminAPI
import de.gematik.ti.directory.util.DirectoryAuthException
import io.ktor.http.*
import io.ktor.http.parsing.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.http.content.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.resources.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

val AdminAPIKey = AttributeKey<AdminAPI>("AdminAPI")

/**
 * Backend for Frontend module for Directory BFF (Backend for Frontend)
 */
fun Application.directoryModule() {
    install(ContentNegotiation) {
        json(
            Json {
                prettyPrint = true
                isLenient = true
            }
        )
    }
    install(Resources)

    attributes.put(AdminAPIKey, AdminAPI())

    routing {
        route("api") {
            vaultRoute()
            adminRoutes()
        }

        singlePageApplication {
            useResources = true
            filesPath = "directory-app"
            defaultPage = "index.html"
        }
    }

    install(StatusPages) {
        exception<ParseException> { call, _ ->
            call.respondText(text = "401: Unauthorized", status = HttpStatusCode.Unauthorized)
        }
        exception<DirectoryAuthException> { call, _ ->
            call.respondText(text = "401: Unauthorized", status = HttpStatusCode.Unauthorized)
        }
    }
}

@Serializable
data class Outcome(val message: String)
