/*
Copyright 2026, gematik GmbH

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

*******

For additional notes and disclaimer from gematik and in case of changes
by gematik, find details in the "Readme" file.
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
