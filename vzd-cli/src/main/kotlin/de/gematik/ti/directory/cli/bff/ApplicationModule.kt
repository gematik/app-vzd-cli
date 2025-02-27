package de.gematik.ti.directory.cli.bff

import de.gematik.ti.directory.DirectoryAuthException
import de.gematik.ti.directory.admin.AdminResponseException
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
import mu.KotlinLogging

val AdminAPIKey = AttributeKey<AdminAPI>("AdminAPI")
val GlobalAPIKey = AttributeKey<GlobalAPI>("GlobalAPI")

val logger = KotlinLogging.logger {}

class Configuration(
    val globalAPI: GlobalAPI,
    val adminAPI: AdminAPI,
    var routing: (Routing.() -> Unit)? = null,
)

/**
 * Backend for Frontend module for Directory BFF (Backend for Frontend)
 */
fun Application.directoryModule(configure: Configuration.() -> Unit = {}) {
    val globalAPI = GlobalAPI()
    val adminAPI = AdminAPI(globalAPI)
    val cfg = Configuration(globalAPI, adminAPI)
    // allow external code to customize the configuration
    configure(cfg)
    attributes.put(GlobalAPIKey, globalAPI)
    attributes.put(AdminAPIKey, adminAPI)

    install(ContentNegotiation) {
        json(
            Json {
                prettyPrint = true
                isLenient = true
                encodeDefaults = true
                serializersModule =
                    SerializersModule {
                        contextual(ExtendedCertificateDataDERSerializer)
                    }
            },
        )
    }
    install(Resources)

    if (cfg.routing != null) {
        // allow overriding the routing
        routing(cfg.routing!!)
    } else {
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
    }

    install(StatusPages) {
        exception<ParseException> { call, cause ->
            call.respond(HttpStatusCode.BadRequest, Outcome("bad_request", cause.message))
        }
        exception<VaultException> { call, _ ->
            call.respond(HttpStatusCode.Unauthorized, Outcome("unauthorized", "Vault error"))
        }
        exception<DirectoryAuthException> { call, cause ->
            call.respond(HttpStatusCode.Unauthorized, Outcome("unauthorized", cause.message ?: "Auth error"))
        }
        exception<AdminResponseException> { call, cause ->
            call.respond(cause.response.status, Outcome("admin_error", cause.details))
        }
    }
}

@Serializable
data class Outcome(
    val code: String,
    val message: String
)

val ApplicationCall.adminAPI: AdminAPI
    get() {
        return application.attributes[AdminAPIKey]
    }

val ApplicationCall.globalAPI: GlobalAPI
    get() {
        return application.attributes[GlobalAPIKey]
    }
