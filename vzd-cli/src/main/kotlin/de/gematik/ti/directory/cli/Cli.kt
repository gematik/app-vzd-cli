package de.gematik.ti.directory.cli

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.options.counted
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.versionOption
import de.gematik.ti.directory.cli.admin.DirectoryAdministrationCli
import de.gematik.ti.directory.cli.gui.GuiCommand
import de.gematik.ti.directory.cli.ldif.LdifCommand
import de.gematik.ti.directory.cli.pers.PersCommand
import kotlinx.serialization.json.Json
import mu.KotlinLogging
import net.mamoe.yamlkt.Yaml
import org.slf4j.LoggerFactory
import java.nio.file.attribute.PosixFilePermission
import kotlin.io.path.*

private val logger = KotlinLogging.logger {}

val JsonPretty = Json { prettyPrint = true }
val YamlPretty = Yaml { encodeDefaultValues = false }

class Cli : CliktCommand(name = "vzd-cli") {
    private val verbosity by option("-v", help = "Display log, use -vv for even more details").counted()

    init {
        versionOption(BuildConfig.APP_VERSION)
        subcommands(DirectoryAdministrationCli(), LdifCommand(), PersCommand(), GuiCommand())
        val configDir = Path(System.getProperty("user.home"), ".telematik")
        if (!configDir.toFile().exists()) {
            configDir.absolute().toFile().mkdirs()
        }
        configDir.setPosixFilePermissions(
            setOf(
                PosixFilePermission.OWNER_READ,
                PosixFilePermission.OWNER_WRITE,
                PosixFilePermission.OWNER_EXECUTE
            )
        )
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
    }
}

fun main(args: Array<String>) = Cli()
    .main(args)
