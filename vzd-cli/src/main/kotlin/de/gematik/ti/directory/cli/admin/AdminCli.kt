package de.gematik.ti.directory.cli.admin

import com.github.ajalt.clikt.core.*
import com.github.ajalt.clikt.parameters.options.*
import com.github.ajalt.clikt.parameters.types.choice
import de.gematik.ti.directory.admin.*
import de.gematik.ti.directory.cli.admin.compat.CmdCommand
import de.gematik.ti.directory.cli.catching
import de.gematik.ti.directory.global.GlobalAPI
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

class CommandContext(
    val adminAPI: AdminAPI,
    private val clientDelegate: CommandContext.() -> Client,
    var outputFormat: OutputFormat,
    val env: AdminEnvironment,
    val enableOcsp: Boolean,
    var firstCommand: Boolean = true
) {

    val client by lazy {
        clientDelegate.invoke(this)
    }

    val pkiClient get() = adminAPI.globalAPI.pkiClient
}

class DirectoryAdministrationCli :
    CliktCommand(name = "admin", help = """CLI for DirectoryAdministration API""".trimMargin()) {
    private val outputFormat by option().switch(
        "--human" to OutputFormat.HUMAN,
        "--json" to OutputFormat.JSON,
        "--yaml" to OutputFormat.YAML,
        "--csv" to OutputFormat.CSV,
        "--short" to OutputFormat.TABLE,
        "--table" to OutputFormat.TABLE
    )
        .default(OutputFormat.HUMAN)
        .deprecated("DEPRECATED: Specify the format on specific sub-command.")

    private val env by option(
        "-e",
        "--env",
        help = "Environment. Either tu, ru or pu. If not specified default env is used."
    )
        .choice("tu", "ru", "pu")
        .deprecated("DEPRECATED: Switch environment globally using vzd-cli admin login <ENV>")

    private val enableOcsp: Boolean by option(
        "-o",
        "--ocsp",
        help = "Validate certificates using OCSP"
    )
        .flag()
        .deprecated("Use --ocsp in particular sub-commands")

    private val useProxy: Boolean? by option(
        "--proxy-on",
        "-x",
        help = "Forces the use of the proxy, overrides the configuration"
    )
        .flag("--proxy-off", "-X")

    override fun run() = catching {
        val adminAPI = AdminAPI(GlobalAPI())
        val clientEnvStr =
            env ?: adminAPI.config.currentEnvironment ?: throw CliktError("Default environment is not configured")

        val clientEnv = AdminEnvironment.valueOf(clientEnvStr.uppercase())

        val clientDelegate: CommandContext.() -> Client = {
            logger.info { "Using environment: $clientEnv" }
            adminAPI.createClient(clientEnv)
        }

        currentContext.obj = CommandContext(adminAPI, clientDelegate, outputFormat, clientEnv, enableOcsp)
    }

    init {
        subcommands(
            VaultCommand(),
            ConfigCommand(),
            LoginCommand(),
            LoginCredCommand(),
            TokenCommand(),
            Info(),
            SearchCommand(),
            ShowCommand(),
            ListCommand(),
            TemplateCommand(),
            AddBaseCommand(),
            LoadBaseCommand(),
            EditBaseCommand(),
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
            CmdCommand(),
        )
    }
}

class Info : CliktCommand(name = "info", help = "Show information about the API") {
    private val context by requireObject<CommandContext>()

    override fun run() = catching {
        val info = runBlocking { context.client.getInfo() }
        Output.printHuman(info)
    }
}
