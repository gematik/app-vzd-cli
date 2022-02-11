package vzd.tools

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.options.counted
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.versionOption
import com.github.ajalt.clikt.parameters.types.path
import io.github.cdimascio.dotenv.dotenv
import mu.KotlinLogging
import org.slf4j.LoggerFactory
import vzd.tools.directoryadministration.cli.DirectoryAdministrationCli
import kotlin.io.path.*
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

private val logger = KotlinLogging.logger {}

class Cli : CliktCommand(name="vzd-cli") {
    private val verbosity by option("-v", help = "Display log, use -vv for even more details").counted()
    private val env by option(help="specify env file", metavar="FILENAME").path(canBeDir = false)
    init {
        versionOption(BuildConfig.APP_VERSION)
    }
    override fun run() {
        val root: Logger = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME) as Logger
        if (verbosity == 1) {
            root.level = Level.INFO
        } else if (verbosity == 2) {
            root.level = Level.DEBUG
        } else {
            root.level = Level.ERROR
        }

        currentContext.obj = dotenv {
            if (env != null) {
                val path = Path(env.toString().replaceFirst("~", System.getProperty("user.home"))).absolute()
                logger.debug { "Loading environment from ${path.absolutePathString()}" }
                directory = path.parent.absolutePathString()
                filename = path.fileName.name
                ignoreIfMissing = false
            } else {
                ignoreIfMissing = true
            }
        }
    }
}

fun main(args: Array<String>) = Cli()
    .subcommands(DirectoryAdministrationCli())
    .main(args)
