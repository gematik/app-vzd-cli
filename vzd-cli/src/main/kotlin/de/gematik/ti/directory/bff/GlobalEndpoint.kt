package de.gematik.ti.directory.bff

import io.ktor.resources.*
import io.ktor.server.application.*
import io.ktor.server.resources.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable

@Serializable
@Resource("global")
class Global {
    @Serializable
    @Resource("status")
    class Status(val parent: Global = Global())

    @Serializable
    @Resource("config")
    class Config(val parent: Global = Global())
}

fun Route.globalRoutes() {

    get<Global.Config> {
        val globalAPI = application.attributes[GlobalAPIKey]
        call.respond(globalAPI.config)
    }

}
