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
import de.gematik.ti.directory.cli.catching
import de.gematik.ti.directory.cli.util.TokenStore

class TokenCommand : CliktCommand(name = "token", help = "Get or set access token") {
    private val context by requireObject<AdminCliEnvironmentContext>()
    private val token by option("-s", "--set", metavar = "ACCESS_TOKEN", help = "Sets OAuth2 Access Token", envvar = "VZD_CLI_ACCESS_TOKEN")

    override fun run() =
        catching {
            val config = context.adminAPI.config

            val envConfig = config.environment(context.env)

            val tokenStore = TokenStore()

            token?.let { tokenStore.addAccessToken(envConfig.apiURL, it) }

            echo(
                tokenStore
                    .accessTokenFor(
                        envConfig.apiURL,
                    )?.accessToken ?: throw CliktError("No token available for environment: ${context.env}"),
            )
        }
}
