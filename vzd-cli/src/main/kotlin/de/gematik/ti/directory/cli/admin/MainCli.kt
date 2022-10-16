package de.gematik.ti.directory.cli.admin

import com.github.ajalt.clikt.core.*
import com.github.ajalt.clikt.parameters.options.*
import com.github.ajalt.clikt.parameters.types.choice
import de.gematik.ti.directory.admin.*
import de.gematik.ti.directory.cli.admin.compat.CmdCommand
import de.gematik.ti.directory.util.PKIClient
import de.gematik.ti.directory.util.TokenStore
import de.gematik.ti.directory.util.VaultException
import io.github.cdimascio.dotenv.Dotenv
import io.ktor.client.network.sockets.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.SerializationException
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * Must love Kotlin - create a simple try / catch function and use in all classes that throws these exceptions
 */
fun catching(throwingBlock: () -> Unit = {}) {
    try {
        throwingBlock()
    } catch (e: VZDResponseException) {
        throw CliktError(e.details)
    } catch (e: SerializationException) {
        throw CliktError(e.message)
    } catch (e: VaultException) {
        throw CliktError(e.message)
    } catch (e: ConnectTimeoutException) {
        throw CliktError("${e.message}. Try using proxy: vzd-cli admin -x ...")
    } catch (e: io.ktor.http.parsing.ParseException) {
        if (e.message.contains("Expected `=` after parameter key ''")) {
            throw CliktError("ACCESS_TOKEN is invalid. Please login again using `vzd-cli admin login`.")
        } else {
            throw e
        }
    } catch (e: IllegalStateException) {
        // dirty, but no other way atm
        if (e.message?.contains("Unsupported byte code, first byte is 0xfc") == true) {
            throw CliktError("ACCESS_TOKEN is invalid. Please login again using `vzd-cli admin login`.")
        } else {
            throw e
        }
    }
}

class CommandContext(
    private val clientDelegate: () -> Client,
    private val pkiClientDelegate: () -> PKIClient,
    var outputFormat: OutputFormat,
    val env: String,
    val useProxy: Boolean,
    val enableOcsp: Boolean,
    var firstCommand: Boolean = true
) {

    val client by lazy {
        clientDelegate.invoke()
    }

    val pkiClient by lazy {
        pkiClientDelegate.invoke()
    }
}

class DirectoryAdministrationCli :
    CliktCommand(name = "admin", help = """CLI for DirectoryAdministration API""".trimMargin()) {
    private val dotenv by requireObject<Dotenv>()
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

    private val useProxy: Boolean? by option(
        "--proxy-on",
        "-x",
        help = "Forces the use of the proxy, overrides the configuration"
    )
        .flag("--proxy-off", "-X")

    override fun run() = catching {
        val provider = FileConfigProvider()
        val tokenStore = TokenStore()
        val clientEnv =
            env ?: provider.config.currentEnvironment ?: throw CliktError("Default environment is not configured")

        val clientDelegate: () -> Client = {
            logger.info { "Using environment: $clientEnv" }
            val envcfg =
                provider.config.environment(clientEnv) ?: throw CliktError("Default environment not configired: $env")

            val clientApiURL = envcfg.apiURL
            val clientAccessToken = tokenStore.accessTokenFor(envcfg.apiURL)
                ?: throw CliktError("You are not logged in. Use 'vzd-cli admin login $clientEnv'")
            val clientHttpProxyURL = provider.config.httpProxy.proxyURL
            var clientHttpProxyEnabled = provider.config.httpProxy.enabled

            if (useProxy == true) {
                clientHttpProxyEnabled = true
            }

            Client {
                apiURL = clientApiURL
                accessToken = clientAccessToken
                if (clientHttpProxyEnabled) {
                    httpProxyURL = clientHttpProxyURL
                }
            }
        }

        val pkiClientDelegate: () -> PKIClient = {
            PKIClient {
                if (provider.config.httpProxy.enabled || useProxy == true) {
                    httpProxyURL = provider.config.httpProxy.proxyURL
                }
            }
        }

        currentContext.obj = CommandContext(clientDelegate, pkiClientDelegate, outputFormat, clientEnv, useProxy == true, enableOcsp)
    }

    init {
        subcommands(
            VaultCommand(), ConfigCommand(),
            LoginCommand(), LoginCredCommand(), TokenCommand(),
            Info(), ListCommand(), TemplateCommand(), AddBaseCommand(),
            LoadBaseCommand(), ModifyBaseCommand(), ModifyBaseAttrCommand(), DeleteCommand(),
            ListCertCommand(), AddCertCommand(), SaveCertCommand(), DeleteCertCommand(), ClearCertCommand(),
            CertInfoCommand(), DumpCommand(), CmdCommand(), SearchCommand(), EditBaseCommand()
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
