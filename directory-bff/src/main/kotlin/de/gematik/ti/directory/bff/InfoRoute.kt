package de.gematik.ti.directory.bff

import io.github.smiley4.ktorswaggerui.dsl.get
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable

@Serializable
data class InfoResource(
    val name: String,
    val version: String,
    val capabilities: List<String>,
)

fun Route.infoRoute() {
    get("info", {
        response {
            HttpStatusCode.OK to {
                description = "The operation was successful"
                body<InfoResource> {
                }
            }
        }
    }) {
        call.respond(InfoResource("Directory API", BuildConfig.APP_VERSION, listOf("core")))
    }
}
