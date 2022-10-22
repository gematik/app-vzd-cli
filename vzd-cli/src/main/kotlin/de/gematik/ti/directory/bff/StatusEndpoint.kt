package de.gematik.ti.directory.bff

import de.gematik.ti.directory.admin.AdminInfo
import io.ktor.http.*
import io.ktor.resources.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.resources.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import kotlinx.serialization.Serializable
import java.util.*

@Serializable
@Resource("status")
class Status

@Serializable
data class StatusRepresentation(
    val admin: AdminInfo
)

fun Route.statusRoute() {
    get<Status> {
        val adminAPI = application.attributes[AdminAPIKey]
        val status = StatusRepresentation(
            adminAPI.info()
        )
        call.respond(status)
    }
}
