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
