@file:UseSerializers(DirectoryEntryExtensionSerializer::class)
package de.gematik.ti.directory.bff

import de.gematik.ti.directory.admin.*
import io.ktor.http.*
import io.ktor.resources.*
import io.ktor.server.application.*
import io.ktor.server.resources.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers

@Serializable
@Resource("admin")
class Admin {
    @Serializable
    @Resource("status")
    class Status(val parent: Admin = Admin())

    @Serializable
    @Resource("{envTitle}")
    class Env(val parent: Admin = Admin(), private val envTitle: String) {
        val env get() = AdminEnvironment.valueOf(envTitle.uppercase())

        @Serializable
        @Resource("search")
        class Search(val parent: Env, val q: String)

        @Resource("entry/{telematikID}")
        @Serializable
        data class Entry(val parent: Env, val telematikID: String)
    }
}

fun Route.adminRoutes() {
    get<Admin.Status> {
        val adminAPI = application.attributes[AdminAPIKey]
        call.respond(adminAPI.status())
    }

    get<Admin.Env.Search> { search ->
        val adminAPI = application.attributes[AdminAPIKey]
        call.respond(adminAPI.createClient(search.parent.env).quickSearch(search.q))
    }

    get<Admin.Env.Entry> { entry ->
        val adminAPI = application.attributes[AdminAPIKey]
        val result = adminAPI.createClient(entry.parent.env).readDirectoryEntryByTelematikID(entry.telematikID)
        if (result != null) {
            call.respond(result)
        }
        call.respond(HttpStatusCode.NotFound, Outcome("Entry with telematikID '${entry.telematikID}' not found in env '${entry.parent.env}'"))
    }
}
