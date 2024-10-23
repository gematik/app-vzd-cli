package de.gematik.ti.directory.cli.bff

import de.gematik.ti.directory.DirectoryAuthException
import de.gematik.ti.directory.cli.GlobalAPI
import de.gematik.ti.directory.cli.admin.AdminAPI
import de.gematik.ti.directory.cli.util.VaultException
import de.gematik.ti.directory.pki.ExtendedCertificateDataDERSerializer
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
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual

val AdminAPIKey = AttributeKey<AdminAPI>("AdminAPI")
val GlobalAPIKey = AttributeKey<GlobalAPI>("GlobalAPI")

/**
 * Backend for Frontend module for Directory BFF (Backend for Frontend)
 */
fun Application.directoryModule() {
    install(ContentNegotiation) {
        json(
            Json {
                prettyPrint = true
                isLenient = true
                serializersModule =
                    SerializersModule {
                        contextual(ExtendedCertificateDataDERSerializer)
                    }
            },
        )
    }
    install(Resources)

    val globalAPI = GlobalAPI()
    val adminAPI = AdminAPI(globalAPI)
    attributes.put(GlobalAPIKey, globalAPI)
    attributes.put(AdminAPIKey, adminAPI)

    routing {
        route("api") {
            globalRoutes()
            vaultRoute()
            adminRoutes()
        }

        route("api/{...}") {
            handle {
                call.respond(HttpStatusCode.NotFound)
            }
        }

        singlePageApplication {
            useResources = true
            filesPath = "directory-app/browser"
            defaultPage = "index.html"
        }
    }

    install(StatusPages) {
        exception<ParseException> { call, _ ->
            call.respondText(text = "401: Unauthorized", status = HttpStatusCode.Unauthorized)
        }
        exception<VaultException> { call, _ ->
            call.respondText(text = "401: Unauthorized", status = HttpStatusCode.Unauthorized)
        }
        exception<DirectoryAuthException> { call, _ ->
            call.respondText(text = "401: Unauthorized", status = HttpStatusCode.Unauthorized)
        }
    }
}

@Serializable
data class Outcome(val code: String, val message: String)

val ApplicationCall.adminAPI: AdminAPI
    get() {
        return application.attributes[AdminAPIKey]
    }

val ApplicationCall.globalAPI: GlobalAPI
    get() {
        return application.attributes[GlobalAPIKey]
    }
