package de.gematik.ti.directory.cli.bff

import de.gematik.ti.directory.DirectoryEnvironment
import de.gematik.ti.directory.admin.*
import de.gematik.ti.directory.elaborate.ElaborateDirectoryEntry
import de.gematik.ti.directory.elaborate.elaborate
import io.ktor.http.*
import io.ktor.resources.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.resources.*
import io.ktor.server.resources.post
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import java.io.IOException

@Serializable
data class LoginWithVaultRepresentation(
    val env: DirectoryEnvironment,
    val vaultPassword: String,
)

@Serializable
@Resource("admin")
class Admin {
    @Serializable
    @Resource("status")
    class Status(val parent: Admin = Admin())

    @Serializable
    @Resource("login")
    class Login(val parent: Admin = Admin())

    @Serializable
    @Resource("{envTitle}")
    class Env(val parent: Admin = Admin(), private val envTitle: String) {
        val env get() = DirectoryEnvironment.valueOf(envTitle)

        @Serializable
        @Resource("search")
        class Search(val parent: Env, val q: String)

        @Resource("entry/{telematikID}")
        @Serializable
        data class Entry(val parent: Env, val telematikID: String)
    }
}

@Serializable
data class ElaboratedSearchResults(
    val searchQuery: String,
    val directoryEntries: List<ElaborateDirectoryEntry>,
)

fun Route.adminRoutes() {
    get<Admin.Status> {
        call.respond(call.adminAPI.status())
    }

    post<Admin.Login> {
        val body = call.receive<LoginWithVaultRepresentation>()

        val vault = call.adminAPI.openVault(body.vaultPassword)
        val credential = vault.get(body.env.toString().lowercase())

        if (credential == null) {
            call.respond(
                HttpStatusCode.BadRequest,
                Outcome("VAULT_CREDENTIALS_MISSING", "Credentials für '${body.env}' are not configured in vault."),
            )
            return@post
        }

        try {
            call.adminAPI.login(body.env, credential.name, credential.secret)
            call.respond(HttpStatusCode.OK, Outcome("VAULT_LOGIN_OK", "Logged in to '${body.env}'"))
        } catch (e: IOException) {
            call.respond(
                HttpStatusCode.BadGateway,
                Outcome("DOWNSTREAM_CONNECTION_ERROR", "Unable to connect to backend. Check proxy settings."),
            )
        }
    }

    get<Admin.Env.Search> { search ->
        val adminAPI = application.attributes[AdminAPIKey]
        val searchResults = adminAPI.createClient(search.parent.env).quickSearch(search.q)
        call.respond(
            ElaboratedSearchResults(
                searchQuery = searchResults.searchQuery,
                directoryEntries = searchResults.directoryEntries.map { it.elaborate() },
            ),
        )
    }

    get<Admin.Env.Entry> { entry ->
        val adminAPI = application.attributes[AdminAPIKey]
        val result = adminAPI.createClient(entry.parent.env).readDirectoryEntryByTelematikID(entry.telematikID)
        if (result != null) {
            call.respond(result.elaborate())
        }
        call.respond(
            HttpStatusCode.NotFound,
            Outcome("NOT_FOUND", "Entry with telematikID '${entry.telematikID}' not found in env '${entry.parent.env}'"),
        )
    }
}
