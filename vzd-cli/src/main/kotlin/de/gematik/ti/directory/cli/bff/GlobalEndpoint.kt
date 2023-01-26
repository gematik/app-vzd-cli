package de.gematik.ti.directory.cli.bff

import de.gematik.ti.directory.cli.GlobalConfig
import io.ktor.resources.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.resources.*
import io.ktor.server.resources.post
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable

@Serializable
@Resource("")
class Global {
    @Serializable
    @Resource("config")
    class Config(val parent: Global = Global())
}

fun Route.globalRoutes() {
    get<Global.Config> {
        call.respond(call.globalAPI.config)
    }
    post<Global.Config> {
        val config = call.receive<GlobalConfig>()
        call.globalAPI.config.updates.preReleasesEnabled = config.updates.preReleasesEnabled
        call.globalAPI.config.httpProxy.enabled = config.httpProxy.enabled
        call.globalAPI.config.httpProxy.proxyURL = config.httpProxy.proxyURL
        call.globalAPI.updateConfig()
        call.respond(call.globalAPI.config)
    }
}
