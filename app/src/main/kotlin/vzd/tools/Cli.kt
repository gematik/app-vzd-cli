package vzd.tools

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.versionOption
import io.github.cdimascio.dotenv.dotenv
import mu.KotlinLogging
import vzd.tools.directoryadministration.cli.DirectoryAdministrationCli
import java.nio.file.Paths

private val logger = KotlinLogging.logger {}

class Cli : CliktCommand(name="vzd-cli") {
    private val env by option(help="specify env file", metavar="FILENAME").default(".env")
    init {
        versionOption(BuildConfig.APP_VERSION)
    }
    override fun run() {
        logger.debug { "Trying to load environment from: $env" }
        currentContext.obj = dotenv {
            directory = Paths.get("").toAbsolutePath().toString()
            filename = env
            ignoreIfMissing = true
        }
    }
}

fun main(args: Array<String>) = Cli()
    .subcommands(DirectoryAdministrationCli())
    .main(args)
