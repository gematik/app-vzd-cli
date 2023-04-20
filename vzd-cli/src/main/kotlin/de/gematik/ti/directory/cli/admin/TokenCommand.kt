package de.gematik.ti.directory.cli.admin

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.CliktError
import com.github.ajalt.clikt.core.requireObject
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import de.gematik.ti.directory.cli.catching
import de.gematik.ti.directory.cli.util.TokenStore

class TokenCommand : CliktCommand(name = "token", help = "Get or set access token") {
    private val context by requireObject<AdminCliEnvironmentContext>()
    private val token by option("-s", "--set", metavar = "ACCESS_TOKEN", help = "Sets OAuth2 Access Token", envvar = "VZD_CLI_ACCESS_TOKEN")

    override fun run() = catching {
        val config = context.adminAPI.config

        val envConfig = config.environment(context.env)

        val tokenStore = TokenStore()

        token?.let { tokenStore.addAccessToken(envConfig.apiURL, it) }

        echo(tokenStore.accessTokenFor(envConfig.apiURL)?.accessToken ?: throw CliktError("No token available for environment: ${context.env}"))
    }
}
