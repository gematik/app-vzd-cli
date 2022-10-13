package de.gematik.ti.directory.cli.admin

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.CliktError
import com.github.ajalt.clikt.output.TermUi
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.prompt
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.choice
import de.gematik.ti.directory.admin.ClientCredentialsAuthenticator
import de.gematik.ti.directory.admin.FileConfigProvider
import de.gematik.ti.directory.util.TokenStore
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonPrimitive
import java.util.*

private fun doLogin(env: String, overrideProxy: Boolean?, clientID: String, clientSecret: String) {
    val provider = FileConfigProvider()
    val envcfg = provider.config.environment(env) ?: throw CliktError("Environment is not configured: $env")
    provider.config.currentEnvironment = env

    provider.config.httpProxy.enabled = when (overrideProxy) {
        true -> {
            true
        }
        false -> {
            false
        }
        else -> {
            provider.config.httpProxy.enabled
        }
    }

    provider.save()

    val auth = ClientCredentialsAuthenticator(
        envcfg.authURL,
        if (provider.config.httpProxy.enabled) provider.config.httpProxy.proxyURL else null
    )
    val authResponse = auth.authenticate(clientID, clientSecret)

    val tokenStore = TokenStore()
    tokenStore.addAccessToken(envcfg.apiURL, authResponse.accessToken)
    val tokenParts = authResponse.accessToken.split(".")
    val tokenBody = String(Base64.getUrlDecoder().decode(tokenParts[1]), Charsets.UTF_8)
    val claims: JsonObject = Json.decodeFromString(tokenBody)

    TermUi.echo("Login successful. Environment set to: $env")

    claims.get("exp")?.jsonPrimitive?.int?.let {
        val expDate = Date(it.toLong() * 1000)
        TermUi.echo("Token valid until $expDate")
    }
}

class LoginCommand : CliktCommand(name = "login", help = "Login to OAuth2 Server and store token(s)") {
    private val env by argument().choice("ru", "tu", "pu")
    private val overrideProxy: Boolean? by option(
        "-x",
        "--proxy-on",
        help = "Forces the use of the proxy, overrides the configuration"
    )
        .flag("--proxy-off", "-X")

    private val password by option("--password", "-p", help = "Password for protection of the Vault")
        .prompt("Enter Vault Password", hideInput = true)

    override fun run() = catching {
        val vault = vaultProvider.open(password)
        val secret = vault.get(env) ?: throw CliktError("Secret for env '$env' not found in Vault:")
        doLogin(env, overrideProxy, secret.clientID, secret.secret)
    }
}

class LoginCredCommand : CliktCommand(name = "login-cred", help = "Login using the client credentials") {
    private val env by argument().choice("ru", "tu", "pu")
    private val overrideProxy: Boolean? by option(
        "-x",
        "--proxy-on",
        help = "Forces the use of the proxy, overrides the configuration"
    )
        .flag("--proxy-off", "-X")
    private val clientId by option("--client-id", "-c", help = "OAuth2 client id", envvar = "CLIENT_ID").required()
    private val clientSecret by option("--secret", "-s", help = "OAuth2 client secret", envvar = "CLIENT_SECRET").required()

    override fun run() = catching {
        doLogin(env, overrideProxy, clientId, clientSecret)
    }
}
