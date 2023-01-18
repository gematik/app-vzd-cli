package de.gematik.ti.directory.bff

import de.gematik.ti.directory.admin.AdminEnvironment
import de.gematik.ti.directory.admin.Client
import de.gematik.ti.directory.admin.ClientCredentialsAuthenticator
import de.gematik.ti.directory.admin.DefaultConfig
import io.ktor.server.engine.*
import io.ktor.server.netty.*

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
    val authentocator = ClientCredentialsAuthenticator(DefaultConfig.environment(env).authURL, null)

    val adminClient = Client {
        apiURL = DefaultConfig.environment(env).apiURL
        auth {
            accessToken {
                authentocator.authenticate(directoryApiAdminClientId, directoryApiAdminClientSecret).accessToken
            }
        }
    }

    embeddedServer(Netty, host = "0.0.0.0", port = directoryApiPort) {
        directoryApplicationModule(adminClient)
    }.start(wait = true)
}
