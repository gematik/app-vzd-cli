package vzd.admin.cli.compat

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.CliktError
import com.github.ajalt.clikt.parameters.options.deprecated
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.path
import de.gematik.ti.epa.vzd.gem.Main
import de.gematik.ti.epa.vzd.gem.exceptions.GemClientException
import de.gematik.ti.epa.vzd.gem.invoker.ConfigHandler
import de.gematik.ti.epa.vzd.gem.utils.GemStringUtils
import mu.KotlinLogging
import org.slf4j.LoggerFactory
import vzd.admin.cli.catching

class CmdCommand : CliktCommand(name = "cmd", help = "Compatibility mode: support for VZDClient XML-commands") {
    private val logger = KotlinLogging.logger {}
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
            commandsFile?.let {
                args.add("-b")
                args.add(commandsFile.toString())
            }
            logger.info("Entering VZD-Client 1.6 compatibility mode")
            Main.main(args.toTypedArray())
        } catch (e: GemClientException) {
            throw CliktError(e.message)
        }
    }
}
