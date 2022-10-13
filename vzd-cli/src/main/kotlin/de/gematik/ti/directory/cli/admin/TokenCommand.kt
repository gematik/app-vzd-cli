package de.gematik.ti.directory.cli.admin

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.CliktError
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.optional
import com.github.ajalt.clikt.parameters.types.choice
import de.gematik.ti.directory.admin.FileConfigProvider
import de.gematik.ti.directory.util.TokenStore

class TokenCommand : CliktCommand(name = "token", help = "Show access token") {
    private val env by argument().choice("ru", "tu", "pu").optional()

    override fun run() = catching {
        val config = FileConfigProvider().config

        val envConfig = config.environment(env) ?: throw CliktError("Environment not configured")

        echo(TokenStore().accessTokenFor(envConfig.apiURL) ?: throw CliktError("No token available for environment: ${env ?: config.currentEnvironment}"))
    }
}
