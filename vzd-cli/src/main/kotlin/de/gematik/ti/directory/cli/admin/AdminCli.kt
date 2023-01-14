package de.gematik.ti.directory.cli.admin

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.requireObject
import com.github.ajalt.clikt.core.subcommands
import de.gematik.ti.directory.cli.GlobalAPI
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

class AdminCli :
    CliktCommand(name = "admin", help = """CLI for DirectoryAdministration API""".trimMargin()) {

    override fun run() = catching {
        val adminAPI = AdminAPI(GlobalAPI())
        currentContext.obj = AdminCliContext(adminAPI)
    }

    init {
        subcommands(
            VaultCommand(),
            ConfigCommand(),
            StatusCommand(),
            CertInfoCommand(),
            EnvironmentCommands(AdminEnvironment.pu),
            EnvironmentCommands(AdminEnvironment.ru),
            EnvironmentCommands(AdminEnvironment.tu),
        )
    }
}

class EnvironmentCommands(env: AdminEnvironment) : CliktCommand(name = env.name, help = """Commands for $env instance""".trimMargin()) {
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
            CertInfoCommand(),
            DumpCommand(),
        )
    }

    override fun run() = catching {
        currentContext.obj = AdminCliEnvironmentContext(context.adminAPI, AdminEnvironment.valueOf(commandName))
    }
}
