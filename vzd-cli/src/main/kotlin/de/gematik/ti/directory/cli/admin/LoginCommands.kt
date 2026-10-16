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

package de.gematik.ti.directory.cli.admin

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.CliktError
import com.github.ajalt.clikt.core.requireObject
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.prompt
import com.github.ajalt.clikt.parameters.options.required
import de.gematik.ti.directory.cli.catching
import java.util.*

private fun doLogin(
    context: AdminCliEnvironmentContext,
    env: AdminEnvironment,
    clientID: String,
    clientSecret: String,
) {
    val claims = context.adminAPI.login(env, clientID, clientSecret, true)

    println("Login to `$env` successful")

    claims["exp"]?.toLong()?.let {
        val expDate = Date(it * 1000)
        println("Token valid until $expDate")
    }
}

class LoginCommand : CliktCommand(name = "login", help = "Login to OAuth2 Server and store token(s)") {
    private val context by requireObject<AdminCliEnvironmentContext>()

    private val password by option(
        "--password",
        "-p",
        help = "Password for protection of the Vault",
        envvar = "VAULT_PASSWORD",
    ).prompt("Enter Vault Password", hideInput = true)

    override fun run() =
        catching {
            val env = context.env
            val vault = context.adminAPI.openVault(password)
            val secret = vault.get(env.toString()) ?: throw CliktError("Secret for env '$env' not found in Vault")
            doLogin(context, env, secret.name, secret.secret)
        }
}

class LoginCredCommand : CliktCommand(name = "login-cred", help = "Login using the client credentials") {
    private val context by requireObject<AdminCliEnvironmentContext>()
    private val clientId by option("-c", "--client-id", help = "OAuth2 client id", envvar = "CLIENT_ID").required()
    private val clientSecret by option("-s", "--secret", help = "OAuth2 client secret", envvar = "CLIENT_SECRET").required()

    override fun run() =
        catching {
            doLogin(context, context.env, clientId, clientSecret)
        }
}
