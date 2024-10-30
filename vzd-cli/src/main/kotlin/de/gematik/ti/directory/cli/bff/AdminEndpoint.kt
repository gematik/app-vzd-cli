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
import io.ktor.server.routing.Route
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.IOException

@Serializable
data class LoginWithVaultRepresentation(
    val env: DirectoryEnvironment,
    val vaultPassword: String,
)

@Serializable
data class Activation(
    val active: Boolean,
)

@Serializable
@Resource("admin")
class Admin {
    @Serializable
    @Resource("status")
    class Status(
        val parent: Admin = Admin()
    )

    @Serializable
    @Resource("login")
    class Login(
        val parent: Admin = Admin()
    )

    @Serializable
    @Resource("{envTitle}")
    class Env(
        val parent: Admin = Admin(),
        private val envTitle: String
    ) {
        val env get() = DirectoryEnvironment.valueOf(envTitle)

        @Serializable
        @Resource("search")
        class Search(
            val parent: Env,
            val q: String
        )

        @Resource("entry/{telematikID}")
        @Serializable
        data class Entry(
            val parent: Env,
            val telematikID: String
        )

        @Resource("entry/{telematikID}/activation")
        @Serializable
        data class EntryActivation(
            val parent: Env,
            val telematikID: String
        )

        @Resource("base-entry/{telematikID}")
        @Serializable
        data class BaseEntry(
            val parent: Env,
            val telematikID: String
        )
    }
}

@Serializable
data class ElaboratedSearchResults(
    val searchQuery: String,
    val directoryEntries: List<ElaborateDirectoryEntry>,
)

private val JsonIgnoreUnknownKeys = Json { ignoreUnknownKeys = true }

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

    get<Admin.Env.Entry> { resource ->
        val adminAPI = application.attributes[AdminAPIKey]

        val cliant = adminAPI.createClient(resource.parent.env)
        // launch two coroutines in parallel and wait for both to finish
        val entry = cliant.readDirectoryEntryByTelematikID(resource.telematikID)
        val logs = cliant.readLog(mapOf("telematikID" to resource.telematikID))

        if (entry != null) {
            val elaborated = entry.elaborate()
            elaborated.logs = logs.map { it.elaborate() }
            call.respond(elaborated)
            return@get
        }
        call.respondText(
            status = HttpStatusCode.NotFound,
            text = "Entry with telematikID '${resource.telematikID}' not found in env '${resource.parent.env}'",
        )
    }

    get<Admin.Env.BaseEntry> { entry ->
        val adminAPI = application.attributes[AdminAPIKey]
        val result = adminAPI.createClient(entry.parent.env).readDirectoryEntryByTelematikID(entry.telematikID)
        if (result != null) {
            call.respond(result.directoryEntryBase)
            return@get
        }
        call.respondText(
            status = HttpStatusCode.NotFound,
            text = "Base entry with telematikID '${entry.telematikID}' not found in env '${entry.parent.env}'",
        )
    }

    put<Admin.Env.BaseEntry> { entry ->
        val adminAPI = application.attributes[AdminAPIKey]
        val client = adminAPI.createClient(entry.parent.env)
        val baseFromClient = call.receive<BaseDirectoryEntry>()

        val jsonData = Json.encodeToString(baseFromClient)
        val updateBaseDirectoryEntry = JsonIgnoreUnknownKeys.decodeFromString<UpdateBaseDirectoryEntry>(jsonData)

        logger.debug { "Updating base entry: $jsonData" }

        val dn = client.modifyDirectoryEntry(baseFromClient.dn?.uid!!, updateBaseDirectoryEntry)

        val updateDirectoryEntry = client.readDirectoryEntry(mapOf("uid" to dn.uid))
        if (updateDirectoryEntry?.first() != null) {
            call.respond(updateDirectoryEntry.first().directoryEntryBase)
            return@put
        }

        call.respondText(
            status = HttpStatusCode.NotFound,
            text = "Base entry with telematikID '${entry.telematikID}' not found in env '${entry.parent.env}'",
        )
    }

    put<Admin.Env.EntryActivation> { resource ->
        val adminAPI = application.attributes[AdminAPIKey]
        val client = adminAPI.createClient(resource.parent.env)
        val activation = call.receive<Activation>()

        val entry = client.readDirectoryEntryByTelematikID(resource.telematikID)

        if (entry != null) {
            client.stateSwitch(entry.directoryEntryBase.dn!!.uid, activation.active)
            call.respond(Outcome("success", "Activation state changed."))
        }
    }
}
