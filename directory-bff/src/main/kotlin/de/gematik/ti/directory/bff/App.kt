package de.gematik.ti.directory.bff

import de.gematik.ti.directory.admin.AdminEnvironment
import de.gematik.ti.directory.admin.Client
import de.gematik.ti.directory.admin.ClientCredentialsAuthenticator
import de.gematik.ti.directory.admin.DefaultConfig
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*

fun main() {
    val directoryApiPort by Setting().int(8080)

    embeddedServer(Netty, host = "0.0.0.0", port = directoryApiPort) {
        directoryApplication()
    }.start(wait = true)

}

fun Application.directoryApplication() {
    val directoryApiAdminEnv by Setting()
    val directoryApiAdminClientId by Setting()
    val directoryApiAdminClientSecret by Setting()

    checkMandatorySettings(
        "DIRECTORY_BFF_ADMIN_ENV",
        "DIRECTORY_BFF_ADMIN_CLIENT_ID",
        "DIRECTORY_BFF_ADMIN_CLIENT_SECRET",
    )
    val env = AdminEnvironment.valueOf(directoryApiAdminEnv)
    val authenticator = ClientCredentialsAuthenticator(DefaultConfig.environment(env).authURL, null)

    val adminClient = Client {
        apiURL = DefaultConfig.environment(env).apiURL
        auth {
            accessToken {
                authenticator.authenticate(directoryApiAdminClientId, directoryApiAdminClientSecret).accessToken
            }
        }
    }

    directoryApplicationModule(adminClient)

}

