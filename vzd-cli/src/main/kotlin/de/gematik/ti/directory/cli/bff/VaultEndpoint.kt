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
import de.gematik.ti.directory.cli.admin.SERVICE_NAME
import de.gematik.ti.directory.cli.util.KeyStoreVaultProvider
import de.gematik.ti.directory.cli.util.VaultException
import io.ktor.http.*
import io.ktor.resources.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.resources.post
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable

@Serializable
@Resource("vault")
class VaultResource

@Serializable
data class OpenVaultRequest(
    val vaultPassword: String
)

fun Route.vaultRoute() {
    post<VaultResource> {
        val vaultPassword = call.receive<OpenVaultRequest>().vaultPassword
        try {
            val vault = KeyStoreVaultProvider().open(vaultPassword, SERVICE_NAME)

            val adminAPI = call.application.attributes[AdminAPIAttributeName]

            vault.list().forEach { secret ->
                application.log.info("Logging in to: ${secret.variant}")
                adminAPI.login(DirectoryEnvironment.valueOf(secret.variant), secret.name, secret.secret)
            }

            call.respond(Outcome("", "Used vault to login to: ${vault.list().map { it.variant }.joinToString()}"))
        } catch (e: VaultException) {
            call.respond(status = HttpStatusCode.Forbidden, Outcome("Unauthorised", ""))
        }
    }
}
