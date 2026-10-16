/*
 * Copyright (Change Date see Readme), gematik GmbH
 *
 * Licensed under the EUPL, Version 1.2 or - as soon they will be approved by the
 * European Commission – subsequent versions of the EUPL (the "Licence").
 * You may not use this work except in compliance with the Licence.
 *
 * You find a copy of the Licence in the "Licence" file or at
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either expressed or implied.
 * In case of changes by gematik find details in the "Readme" file.
 *
 * See the Licence for the specific language governing permissions and limitations under the Licence.
 *
 * *******
 *
 * For additional notes and disclaimer from gematik and in case of changes by gematik,
 * find details in the "Readme" file.
 */

package de.gematik.ti.directory.cli.bff

import de.gematik.ti.directory.DirectoryEnvironment
import de.gematik.ti.directory.cli.GlobalConfig
import de.gematik.ti.directory.cli.doLoginFhirHolder
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
@Resource("")
class Global {
    @Serializable
    @Resource("config")
    class Config(
        val parent: Global = Global()
    )

    @Serializable
    @Resource("login")
    class Login(
        val parent: Admin = Admin()
    )
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
    post<Global.Login> {
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
            logger.info { "Logged in AdminAPI: ${body.env}" }
            doLoginFhirHolder(call.adminAPI, call.fhirAPI, body.env)
            logger.info { "Logged in FhirAPI as Holder: ${body.env}" }
            call.respond(HttpStatusCode.OK, Outcome("VAULT_LOGIN_OK", "Logged in to '${body.env}'"))
        } catch (e: IOException) {
            call.respond(
                HttpStatusCode.BadGateway,
                Outcome("DOWNSTREAM_CONNECTION_ERROR", "Unable to connect to backend. Check proxy settings."),
            )
        }
    }
}
