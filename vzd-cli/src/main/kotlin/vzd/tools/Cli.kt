package vzd.tools

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.versionOption
import io.github.cdimascio.dotenv.dotenv
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import mu.KotlinLogging
import vzd.tools.directoryadministration.cli.DirectoryAdministrationCli
import java.nio.file.Paths
import kotlin.io.path.absolute
import kotlin.io.path.absolutePathString
import kotlin.io.path.name
import kotlin.io.path.pathString

private val logger = KotlinLogging.logger {}

class Cli : CliktCommand(name="vzd-cli") {
    private val env by option(help="specify env file", metavar="FILENAME")
    init {
        versionOption(BuildConfig.APP_VERSION)
    }
    override fun run() {
        currentContext.obj = dotenv {
            if (env != null) {
                val path = Paths.get(env!!.replaceFirst("~", System.getProperty("user.home"))).absolute()
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
