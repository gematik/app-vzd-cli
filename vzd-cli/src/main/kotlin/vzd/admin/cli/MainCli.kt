package vzd.admin.cli

import com.github.ajalt.clikt.core.*
import com.github.ajalt.clikt.parameters.groups.provideDelegate
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.prompt
import com.github.ajalt.clikt.parameters.options.switch
import com.github.ajalt.clikt.parameters.types.choice
import io.github.cdimascio.dotenv.Dotenv
import io.ktor.client.plugins.auth.providers.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import mu.KotlinLogging
import vzd.admin.client.*
import java.util.*

private val logger = KotlinLogging.logger {}
private val JSON = Json { prettyPrint = true }

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
    }
}

enum class OutputFormat {
    HUMAN, JSON, YAML, CSV, SHORT
}

class CommandContext(
    val clientDelegate: () -> Client,
    val outputFormat: OutputFormat,
    val env: String,
    var firstCommand: Boolean = true,
) {

    val client by lazy {
        clientDelegate.invoke()
    }

}

class DirectoryAdministrationCli :
    CliktCommand(name = "admin", allowMultipleSubcommands = true, help = """CLI for DirectoryAdministration API""".trimMargin()) {
    private val dotenv by requireObject<Dotenv>()
    private val outputFormat by option().switch(
        "--human" to OutputFormat.HUMAN,
        "--json" to OutputFormat.JSON,
        "--yaml" to OutputFormat.YAML,
        "--csv" to OutputFormat.CSV,
        "--short" to OutputFormat.SHORT,
    ).default(OutputFormat.HUMAN)

    private val env by option("-e", "--env", help="Environment. Either tu, ru or pu. If not specified default env is used.").choice("tu", "ru", "pu")

    override fun run() = catching {

        val provider = FileConfigProvider()
        val clientEnv =
            env ?: provider.config.currentEnvironment ?: throw CliktError("Default environment is not configured")

        logger.info { "Using environment: $clientEnv" }

        var clientDelegate: () -> Client = {
            val envcfg =
                provider.config.environment(clientEnv) ?: throw CliktError("Default environment not configired: $env")

            var clientApiURL = envcfg.apiURL
            var clientAccessToken = provider.config.tokens?.get(clientEnv)?.accessToken
            var clientHttpProxyURL = provider.config.httpProxy?.proxyURL
            var clientHttpProxyEnabled = provider.config.httpProxy?.enabled

            dotenv.get("ADMIN_API_URL", null)?.let {
                logger.error { "ADMIN_API_URL is deprecated, use admin config instead" }
                clientApiURL = it
            }
            dotenv.get("HTTP_PROXY_URL", null)?.let {
                logger.error { "HTTP_PROXY_URL is deprecated, use 'vzd-cli admin config' instead" }
                clientHttpProxyURL = it
                clientHttpProxyEnabled = true
            }
            dotenv.get("ADMIN_ACCESS_TOKEN", null)?.let {
                logger.error { "ADMIN_ACCESS_TOKEN is deprecated, use 'vzd-cli admin config' instead" }
                clientAccessToken = it
            }

            if (clientAccessToken == null) {
                dotenv.get("ADMIN_CLIENT_SECRET", null)?.let {
                    logger.error { "ADMIN_CLIENT_SECRET is deprecated, use 'vzd-cli admin vault' instead" }
                    val auth = ClientCredentialsAuthenticator(
                        dotenv["ADMIN_AUTH_URL"] ?: throw UsageError("Environment variable ADMIN_AUTH_URL is not set"),
                        dotenv.get("HTTP_PROXY_URL", null)
                    )
                    val authResponse = auth.authenticate(
                        dotenv["ADMIN_CLIENT_ID"]
                            ?: throw UsageError("Environment variable ADMIN_CLIENT_ID is not set"),
                        dotenv["ADMIN_CLIENT_SECRET"]
                    )
                    clientAccessToken = authResponse.accessToken
                }
            }

            if (clientAccessToken == null) {
                throw CliktError("You are not logged in. Use 'vzd-cli admin -e $clientEnv login'")
            }

            Client {
                apiURL = clientApiURL
                accessToken = clientAccessToken!!
                if (clientHttpProxyEnabled == true) {
                    httpProxyURL = clientHttpProxyURL
                }
            }
        }

        currentContext.obj = CommandContext(clientDelegate, outputFormat, clientEnv)
    }

    init {
        subcommands(VaultCommand(), ConfigCommand(),
            LoginCommand(), AuthenticateAdmin(),
            Info(), ListCommand(), TempolateCommand(), AddBaseCommand(),
            LoadBaseCommand(), ModifyBaseDirectoryEntry(), ModifyBaseAttrCommand(), DeleteCommand(),
            ListCertCommand(), AddCertCommand(), SaveCertCommand(), DeleteCertCommand(), ClearCertCommand(), CertInfoCommand())
    }

}

class AuthenticateAdmin : CliktCommand(name = "auth", help = "Show current access token") {
    private val context by requireObject<CommandContext>()

    override fun run() = catching {
        echo(context.client.accessToken)
    }
}

class Info : CliktCommand(name = "info", help = "Show information about the API") {
    private val context by requireObject<CommandContext>()

    override fun run() = catching {
        val info = runBlocking { context.client.getInfo() }
        when (context.outputFormat) {
            OutputFormat.JSON -> Output.printJson(info)
            OutputFormat.HUMAN, OutputFormat.YAML -> Output.printYaml(info)
            else -> throw UsageError("Info is not available for format: ${context.outputFormat}")
        }
    }

}

class LoginCommand: CliktCommand(name = "login", help = "Login to OAuth2 Server and store token(s)") {
    private val context by requireObject<CommandContext>()
    private val vaultOptions by VaultCommonOptions()

    override fun run() = catching {
        val provider = FileConfigProvider()
        val envcfg = provider.config.environment(context.env) ?: throw CliktError("Environment is not configured: ${context.env}")
        val vault = KeyStoreVaultProvider(vaultOptions.password)
        val auth = ClientCredentialsAuthenticator(envcfg.authURL,
            if (provider.config.httpProxy?.enabled == true) provider.config.httpProxy?.proxyURL else null )
        val secret = vault.get(context.env) ?: throw CliktError("Secret für env '${context.env}' not found in Vault:")
        val authResponse = auth.authenticate(secret.clientID, secret.secret)
        val tokens = provider.config.tokens ?: emptyMap<String, TokenConfig>()
        provider.config.tokens = tokens + mapOf(context.env to TokenConfig(accessToken = authResponse.accessToken))
        provider.save()
        // show token as pretty json
        val tokenParts = authResponse.accessToken.split(".")
        val tokenBody = String(Base64.getUrlDecoder().decode(tokenParts[1]), Charsets.UTF_8)
        val j: JsonObject = Json.decodeFromString(tokenBody)
        echo(JSON.encodeToString(j))
    }
}