package de.gematik.ti.directory.cli.admin

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.CliktError
import com.github.ajalt.clikt.core.requireObject
import com.github.ajalt.clikt.parameters.groups.provideDelegate
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.prompt
import com.github.ajalt.clikt.parameters.options.required
import de.gematik.ti.directory.admin.AdminEnvironment
import de.gematik.ti.directory.cli.ProxyOptions
import de.gematik.ti.directory.cli.catching
import java.util.*

private fun doLogin(context: AdminCliEnvironmentContext, env: AdminEnvironment, overrideProxy: Boolean?, clientID: String, clientSecret: String) {
    context.adminAPI.globalAPI.config.httpProxy.enabled = overrideProxy ?: context.adminAPI.globalAPI.config.httpProxy.enabled
    context.adminAPI.globalAPI.updateConfig()

    val claims = context.adminAPI.login(env, clientID, clientSecret)

    println("Login successful. Environment set to: $env")

    claims["exp"]?.toLong()?.let {
        val expDate = Date(it * 1000)
        println("Token valid until $expDate")
    }
}

class LoginCommand : CliktCommand(name = "login", help = "Login to OAuth2 Server and store token(s)") {
    private val context by requireObject<AdminCliEnvironmentContext>()
    private val proxyOptions by ProxyOptions()

    private val password by option("--password", "-p", help = "Password for protection of the Vault")
        .prompt("Enter Vault Password", hideInput = true)

    override fun run() = catching {
        val env = context.env
        val vault = context.adminAPI.openVault(password)
        val secret = vault.get(env.toString()) ?: throw CliktError("Secret for env '$env' not found in Vault")
        doLogin(context, env, proxyOptions.enableProxy, secret.name, secret.secret)
    }
}

class LoginCredCommand : CliktCommand(name = "login-cred", help = "Login using the client credentials") {
    private val context by requireObject<AdminCliEnvironmentContext>()
    private val proxyOptions by ProxyOptions()
    private val clientId by option("-c", "--client-id", help = "OAuth2 client id", envvar = "CLIENT_ID").required()
    private val clientSecret by option("-s", "--secret", help = "OAuth2 client secret", envvar = "CLIENT_SECRET").required()

    override fun run() = catching {
        doLogin(context, context.env, proxyOptions.enableProxy, clientId, clientSecret)
    }
}
