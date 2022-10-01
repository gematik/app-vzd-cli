package de.gematik.ti.directory.cli

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.options.counted
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.versionOption
import com.github.ajalt.clikt.parameters.types.path
import de.gematik.ti.directory.admin.cli.DirectoryAdministrationCli
import de.gematik.ti.directory.cli.gui.GuiCommand
import de.gematik.ti.directory.cli.ldif.LdifCommand
import de.gematik.ti.directory.cli.pers.PersCommand
import io.github.cdimascio.dotenv.dotenv
import kotlinx.serialization.json.Json
import mu.KotlinLogging
import net.mamoe.yamlkt.Yaml
import org.slf4j.LoggerFactory
import kotlin.io.path.Path
import kotlin.io.path.absolute
import kotlin.io.path.absolutePathString
import kotlin.io.path.name

private val logger = KotlinLogging.logger {}

val JsonPretty = Json { prettyPrint = true }
val YamlPretty = Yaml { encodeDefaultValues = false }

class Cli : CliktCommand(name = "vzd-cli") {
    private val verbosity by option("-v", help = "Display log, use -vv for even more details").counted()
    private val env by option(help = "specify env file", metavar = "FILENAME").path(canBeDir = false)

    init {
        versionOption(BuildConfig.APP_VERSION)
        subcommands(DirectoryAdministrationCli(), LdifCommand(), PersCommand(), GuiCommand())
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
    .main(args)
