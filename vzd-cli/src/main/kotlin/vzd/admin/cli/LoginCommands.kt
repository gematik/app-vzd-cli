package vzd.admin.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.CliktError
import com.github.ajalt.clikt.core.requireObject
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.prompt
import com.github.ajalt.clikt.parameters.options.required
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import vzd.admin.client.*
import java.util.*

private fun doLogin(context: CommandContext, clientID: String, clientSecret: String): JsonObject {
    val provider = FileConfigProvider()
    val envcfg = provider.config.environment(context.env) ?: throw CliktError("Environment is not configured: ${context.env}")
    val vaultProvider = KeyStoreVaultProvider()
    if (!vaultProvider.exists()) throw CliktError("Vault is not initialized. See vzd-cli admin vault --help")
    val auth = ClientCredentialsAuthenticator(
        envcfg.authURL,
        if (provider.config.httpProxy?.enabled == true || context.useProxy) provider.config.httpProxy?.proxyURL else null
    )
    val authResponse = auth.authenticate(clientID, clientSecret)
    val tokens = provider.config.tokens ?: emptyMap()
    provider.config.tokens = tokens + mapOf(context.env to TokenConfig(accessToken = authResponse.accessToken))
    provider.save()
    val tokenParts = authResponse.accessToken.split(".")
    val tokenBody = String(Base64.getUrlDecoder().decode(tokenParts[1]), Charsets.UTF_8)
    return Json.decodeFromString(tokenBody)
}

class LoginCommand : CliktCommand(name = "login", help = "Login to OAuth2 Server and store token(s)") {
    private val context by requireObject<CommandContext>()
    private val password by option("--password", "-p", help = "Password for protection of the Vault")
        .prompt("Enter Vault Password", hideInput = true)

    override fun run() = catching {
        val vault = vaultProvider.open(password)
        val secret = vault.get(context.env) ?: throw CliktError("Secret for env '${context.env}' not found in Vault:")
        echo(JsonPretty.encodeToString(doLogin(context, secret.clientID, secret.secret)))
    }
}

class LoginCredCommand : CliktCommand(name = "login-cred", help = "Login using the client credentials") {
    private val context by requireObject<CommandContext>()
    private val clientId by option("--client-id", "-c", help = "OAuth2 client id", envvar = "CLIENT_ID").required()
    private val clientSecret by option("--secret", "-s", help = "OAuth2 client secret", envvar = "CLIENT_SECRET").required()

    override fun run() = catching {
        echo(JsonPretty.encodeToString(doLogin(context, clientId, clientSecret)))
    }
}
