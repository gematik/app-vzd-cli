package vzd.tools

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import io.github.cdimascio.dotenv.dotenv
import mu.KotlinLogging
import vzd.tools.directoryadministration.DirectoryAdministrationCli
import java.nio.file.Paths

private val logger = KotlinLogging.logger {}
public var dotenv = dotenv { ignoreIfMissing=true }

class Cli : CliktCommand(name="vzd/tools") {
    private val env by option(help="specify env file", metavar="FILENAME").default(".env")
    override fun run() {
        logger.debug { "Trying to load environment from: $env" }
        dotenv = dotenv {
            directory = Paths.get("").toAbsolutePath().toString()
            filename = env
            ignoreIfMissing = true
        }
    }
}

fun main(args: Array<String>) = Cli()
    .subcommands(DirectoryAdministrationCli())
    .main(args)
