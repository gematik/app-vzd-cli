package de.gematik.ti.directory.cli.admin

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.CliktError
import com.github.ajalt.clikt.core.requireObject
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.optional
import com.github.ajalt.clikt.parameters.types.choice
import de.gematik.ti.directory.cli.catching
import de.gematik.ti.directory.util.TokenStore

class TokenCommand : CliktCommand(name = "token", help = "Show access token") {
    private val context by requireObject<CommandContext>()
    private val env by argument().choice("ru", "tu", "pu").optional()

    override fun run() = catching {
        val config = context.adminAPI.config

        val envConfig = config.environment(env)

        echo(TokenStore().accessTokenFor(envConfig.apiURL)?.accessToken ?: throw CliktError("No token available for environment: ${env ?: config.currentEnvironment}"))
    }
}
