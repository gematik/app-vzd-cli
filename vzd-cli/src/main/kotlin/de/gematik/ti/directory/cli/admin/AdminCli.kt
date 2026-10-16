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

package de.gematik.ti.directory.cli.admin

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.requireObject
import com.github.ajalt.clikt.core.subcommands
import de.gematik.ti.directory.cli.GlobalAPI
import de.gematik.ti.directory.cli.VaultCommand
import de.gematik.ti.directory.cli.catching
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

class AdminCliContext(
    val adminAPI: AdminAPI,
)

class AdminCliEnvironmentContext(
    val adminAPI: AdminAPI,
    var env: AdminEnvironment,
) {
    val client by lazy {
        adminAPI.createClient(env)
    }

    val pkiClient get() = adminAPI.globalAPI.pkiClient
}

class AdminCli : CliktCommand(name = "admin", help = """CLI for DirectoryAdministration API""".trimMargin()) {
    override fun run() =
        catching {
            val adminAPI = AdminAPI(GlobalAPI())
            currentContext.obj = AdminCliContext(adminAPI)
        }

    init {
        subcommands(
            VaultCommand(SERVICE_NAME),
            ConfigCommand(),
            StatusCommand(),
            CertInfoCommand(),
            EnvironmentCommands(AdminEnvironment.pu),
            EnvironmentCommands(AdminEnvironment.ru),
            EnvironmentCommands(AdminEnvironment.tu),
        )
    }
}

class EnvironmentCommands(
    env: AdminEnvironment
) : CliktCommand(name = env.name, help = """Commands for $env instance""".trimMargin()) {
    private val context by requireObject<AdminCliContext>()

    init {
        subcommands(
            LoginCommand(),
            LoginCredCommand(),
            TokenCommand(),
            SearchCommand(),
            ShowCommand(),
            ListCommand(),
            TemplateCommand(),
            AddBaseCommand(),
            LoadBaseCommand(),
            EditCommand(),
            ModifyBaseCommand(),
            ModifyBaseAttrCommand(),
            DeleteCommand(),
            ListCertCommand(),
            AddCertCommand(),
            SaveCertCommand(),
            DeleteCertCommand(),
            ClearCertCommand(),
            DumpCommand(),
            LogCommand(),
            DeactivateCommand(),
            ActivateCommand(),
        )
    }

    override fun run() =
        catching {
            currentContext.obj = AdminCliEnvironmentContext(context.adminAPI, AdminEnvironment.valueOf(commandName))
        }
}
