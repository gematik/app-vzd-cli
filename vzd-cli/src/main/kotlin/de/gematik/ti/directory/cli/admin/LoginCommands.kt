package de.gematik.ti.directory.cli.admin

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.CliktError
import com.github.ajalt.clikt.core.requireObject
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.prompt
import com.github.ajalt.clikt.parameters.options.required
import de.gematik.ti.directory.admin.AdminEnvironment
import de.gematik.ti.directory.cli.catching
import java.util.*

private fun doLogin(
    context: AdminCliEnvironmentContext,
    env: AdminEnvironment,
    clientID: String,
    clientSecret: String
) {
    val claims = context.adminAPI.login(env, clientID, clientSecret)

    println("Login to `$env` successful")

    claims["exp"]?.toLong()?.let {
        val expDate = Date(it * 1000)
        println("Token valid until $expDate")
    }
}

class LoginCommand : CliktCommand(name = "login", help = "Login to OAuth2 Server and store token(s)") {
    private val context by requireObject<AdminCliEnvironmentContext>()

    private val password by option(
        "--password", "-p",
        help = "Password for protection of the Vault",
        envvar = "VAULT_PASSWORD"
    ).prompt("Enter Vault Password", hideInput = true)

    override fun run() = catching {
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

    override fun run() = catching {
        doLogin(context, context.env, clientId, clientSecret)
    }
}
