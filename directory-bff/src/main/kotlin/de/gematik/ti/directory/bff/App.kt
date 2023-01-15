package de.gematik.ti.directory.bff

import de.gematik.spegg.Setting
import de.gematik.spegg.checkMandatorySettings
import de.gematik.ti.directory.admin.*
import io.ktor.client.plugins.auth.providers.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable

fun main() {
    val directoryApiPort by Setting().int(8080)
    val directoryApiAdminEnv by Setting()
    val directoryApiAdminClientId by Setting()
    val directoryApiAdminClientSecret by Setting()

    checkMandatorySettings(
        "DIRECTORY_API_ADMIN_ENV",
        "DIRECTORY_API_ADMIN_CLIENT_ID",
        "DIRECTORY_API_ADMIN_CLIENT_SECRET",
    )

    val env = AdminEnvironment.valueOf(directoryApiAdminEnv)
    val auth = ClientCredentialsAuthenticator(DefaultConfig.environment(env).authURL, null)

    val adminClient = Client {
        apiURL = DefaultConfig.environment(env).apiURL
        auth {
            accessToken {
                auth.authenticate(directoryApiAdminClientId, directoryApiAdminClientSecret).accessToken
            }
        }
    }

    embeddedServer(Netty, host = "0.0.0.0", port = directoryApiPort) {
        install(Authentication) {
        }
        install(ContentNegotiation) {
            json()
        }
        defaultRoutes()

        /*
        install(StatusPages) {
            exception<SocketException> { call, cause ->
                call.respondText(text = "$cause" , status = HttpStatusCode.BadGateway)
            }
        }
        */
    }.start(wait = true)
}

@Serializable
data class InfoResource(val name: String, val version: String)

fun Application.defaultRoutes() {
    routing {
        get("/") {
            call.respond(InfoResource("Directory API", BuildConfig.APP_VERSION))
        }
    }
}
