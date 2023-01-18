package de.gematik.ti.directory.bff

import de.gematik.ti.directory.admin.Client
import de.gematik.ti.directory.pki.ExtendedCertificateDataDERSerializer
import io.github.smiley4.ktorswaggerui.SwaggerUI
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual

fun Application.directoryApplicationModule(adminClient: Client) {
    attributes.put(AdminClientAttributeKey, adminClient)
    install(SwaggerUI) {
        swagger {
            swaggerUrl = "/api/swagger-ui"
            onlineSpecValidator()
        }
        info {
            title = "Directory Backend-for-Frontend API"
            version = BuildConfig.APP_VERSION
        }
        server {
            url = "http://localhost:8080"
            description = "Development server"
        }
    }
    install(ContentNegotiation) {
        json(
            Json {
                encodeDefaults = false
                serializersModule = SerializersModule {
                    contextual(ExtendedCertificateDataDERSerializer)
                }
            },
        )
    }

    install(StatusPages) {
        exception<RequestException> { call, cause ->
            call.respond(status = cause.code, cause.response())
        }
        status(HttpStatusCode.InternalServerError) { call, code ->
            call.respond(status = code, ErrorResponse("internal_server_error", "Internal Server Error"))
        }
    }
    routing() {
        route("api") {
            infoRoute()
            searchRoute()
        }
    }
}

val AdminClientAttributeKey = AttributeKey<Client>("AdminClient")
val ApplicationCall.adminClient: Client
    get() {
        return application.attributes[AdminClientAttributeKey]
    }
