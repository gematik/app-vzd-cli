package de.gematik.ti.directory.cli.admin.compat

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.ConsoleAppender
import ch.qos.logback.core.FileAppender
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.CliktError
import com.github.ajalt.clikt.core.requireObject
import com.github.ajalt.clikt.parameters.options.deprecated
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.path
import de.gematik.ti.directory.cli.admin.CommandContext
import de.gematik.ti.directory.cli.admin.catching
import de.gematik.ti.epa.vzd.client.invoker.auth.OAuth
import de.gematik.ti.epa.vzd.gem.Main
import de.gematik.ti.epa.vzd.gem.exceptions.GemClientException
import de.gematik.ti.epa.vzd.gem.invoker.TokenProvider
import mu.KotlinLogging
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class CliTokenProvider(private val oauth: OAuth) : TokenProvider {
    constructor(accessToken: String) : this(OAuth()) {
        this.oauth.accessToken = accessToken
    }
    override fun getOAuth2Token(): OAuth {
        return oauth
    }
}

class CmdCommand : CliktCommand(name = "cmd", help = "Compatibility mode: support for VZDClient XML-commands") {
    private val logger = KotlinLogging.logger {}
    private val context by requireObject<CommandContext>()
    private val paramFile by option(
        "-p",
        "--params",
        metavar = "CONF_FILE",
        help = "Configuration file containing the config parameters"
    ).path(mustExist = true, mustBeReadable = true, canBeDir = false)
    private val credentialsFile by option(
        "-c",
        "--cred",
        metavar = "CREDENTIALS_FILE",
        help = "File containing the access credentials"
    ).path(mustExist = true, mustBeReadable = true, canBeDir = false).deprecated("Unencrypted credentials are deprecated.\nPlease manage credentials using `vzd-cli admin vault` commands.")
    private val commandsFile by option(
        "-b",
        "--batch",
        metavar = "COMMANDS_FILE",
        help = "XML file containing the commands"
    ).path(mustExist = true, mustBeReadable = true, canBeDir = false)

    override fun run() = catching {
        val root: Logger = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME) as Logger

        root.level = Level.DEBUG

        val defaultAppender = root.getAppender("STDERR") as ConsoleAppender

        val fileAppender = FileAppender<ILoggingEvent>()
        fileAppender.encoder = defaultAppender.encoder
        fileAppender.context = defaultAppender.context
        fileAppender.isAppend = true
        fileAppender.file = "logs/vzd-${DateTimeFormatter.ofPattern("yyyy-MM-dd").format(LocalDate.now())}.log"
        fileAppender.start()

        root.addAppender(fileAppender)

        try {
            val args = mutableListOf<String>()
            paramFile?.let {
                args.add("-p")
                args.add(paramFile.toString())
            }
            credentialsFile?.let {
                args.add("-c")
                args.add(credentialsFile.toString())
            }

            logger.info("Entering VZD-Client 1.6 compatibility mode")
            Main.start(args.toTypedArray()) { configHandler ->
                commandsFile?.let {
                    configHandler.commandsPath = it.toAbsolutePath().toString()
                }
                credentialsFile ?: run {
                    configHandler.tokenProvider = CliTokenProvider(context.client.accessToken)
                }
            }
        } catch (e: GemClientException) {
            throw CliktError(e.message)
        }
    }
}
