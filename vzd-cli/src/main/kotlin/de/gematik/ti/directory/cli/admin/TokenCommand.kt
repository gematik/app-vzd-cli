package de.gematik.ti.directory.cli.admin

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.CliktError
import com.github.ajalt.clikt.core.requireObject
import de.gematik.ti.directory.cli.catching
import de.gematik.ti.directory.cli.util.TokenStore

class TokenCommand : CliktCommand(name = "token", help = "Show access token") {
    private val context by requireObject<AdminCliEnvironmentContext>()

    override fun run() = catching {
        val config = context.adminAPI.config

        val envConfig = config.environment(context.env)

        echo(TokenStore().accessTokenFor(envConfig.apiURL)?.accessToken ?: throw CliktError("No token available for environment: ${context.env}"))
    }
}
