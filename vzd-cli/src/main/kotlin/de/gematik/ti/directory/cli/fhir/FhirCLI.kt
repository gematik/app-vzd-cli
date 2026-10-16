/*
 * Copyright (Change Date see Readme), gematik GmbH
 *
 * Licensed under the EUPL, Version 1.2 or - as soon they will be approved by the
 * European Commission – subsequent versions of the EUPL (the "Licence").
 * You may not use this work except in compliance with the Licence.
 *
 * You find a copy of the Licence in the "Licence" file or at
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either expressed or implied.
 * In case of changes by gematik find details in the "Readme" file.
 *
 * See the Licence for the specific language governing permissions and limitations under the Licence.
 *
 * *******
 *
 * For additional notes and disclaimer from gematik and in case of changes by gematik,
 * find details in the "Readme" file.
 */

package de.gematik.ti.directory.cli.fhir

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.requireObject
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.options.option
import de.gematik.ti.directory.DirectoryEnvironment
import de.gematik.ti.directory.cli.EditCommand
import de.gematik.ti.directory.cli.GlobalAPI
import de.gematik.ti.directory.cli.VaultCommand
import de.gematik.ti.directory.cli.catching
import mu.KotlinLogging

val logger = KotlinLogging.logger {}

class FhirCliContext(
    val fhirAPI: FhirAPI,
)

class FhirCliEnvironmentContext(
    val fhirAPI: FhirAPI,
    var env: DirectoryEnvironment,
) {
    val client by lazy {
        fhirAPI.createClient(env)
    }
}

class FhirCli : CliktCommand(name = "fhir", help = """CLI for FHIR Directory""".trimMargin()) {
    override fun run() =
        catching {
            val adminAPI = FhirAPI(GlobalAPI())
            currentContext.obj = FhirCliContext(adminAPI)
        }

    init {
        subcommands(
            VaultCommand(FDV_SEARCH_SERVICE_NAME, "fdv-vault", "Manage FDVSearchAPI credentials in the Vault"),
            EnvironmentCommands(DirectoryEnvironment.pu),
            EnvironmentCommands(DirectoryEnvironment.ru),
            EnvironmentCommands(DirectoryEnvironment.tu),
        )
    }
}

class EnvironmentCommands(
    env: DirectoryEnvironment
) : CliktCommand(name = env.name, help = """Commands for $env instance""".trimMargin()) {
    private val context by requireObject<FhirCliContext>()

    init {
        subcommands(
            SearchTokenCommand(),
            FdvCommands(),
            SearchCommand { ctx, query ->
                ctx.client.search(query)
            },
            ShowCommand(),
            EditCommand(),
            DownloadCommand(),
            UploadCommand(),
        )
    }

    override fun run() =
        catching {
            currentContext.obj = FhirCliEnvironmentContext(context.fhirAPI, DirectoryEnvironment.valueOf(commandName))
        }
}

class SearchTokenCommand : CliktCommand(name = "token", help = "Set or get Access Token for SearchAPI") {
    private val context by requireObject<FhirCliEnvironmentContext>()
    private val token by option("-s", "--set", metavar = "ACCESS_TOKEN", help = "Sets OAuth2 Access Token", envvar = "SEARCH_ACCESS_TOKEN")

    override fun run() =
        catching {
            token?.let { context.fhirAPI.storeAccessTokenSearch(context.env, it) }

            echo(
                context.fhirAPI.retrieveAccessTokenSearch(context.env),
            )
        }
}
