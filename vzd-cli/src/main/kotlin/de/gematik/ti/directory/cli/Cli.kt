package de.gematik.ti.directory.cli

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.CliktError
import com.github.ajalt.clikt.core.context
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.output.CliktHelpFormatter
import com.github.ajalt.clikt.parameters.options.counted
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.versionOption
import de.gematik.ti.directory.admin.AdminResponseException
import de.gematik.ti.directory.apo.ApoCli
import de.gematik.ti.directory.cli.admin.DirectoryAdministrationCli
import de.gematik.ti.directory.cli.global.GlobalCommand
import de.gematik.ti.directory.cli.gui.GuiCommand
import de.gematik.ti.directory.cli.ldif.LdifCommand
import de.gematik.ti.directory.cli.pers.PersCommand
import de.gematik.ti.directory.util.DirectoryException
import de.gematik.ti.directory.util.VaultException
import io.ktor.client.network.sockets.*
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import mu.KotlinLogging
import net.mamoe.yamlkt.Yaml
import org.slf4j.LoggerFactory
import java.nio.file.attribute.PosixFilePermission
import kotlin.io.path.*

private val logger = KotlinLogging.logger {}

val JsonPretty = Json { prettyPrint = true }
val YamlPretty = Yaml { encodeDefaultValues = false }

/**
 * Must love Kotlin - create a simple try / catch function and use in all classes that throws these exceptions
 */
fun catching(throwingBlock: () -> Unit = {}) {
    try {
        throwingBlock()
    } catch (e: DirectoryException) {
        throw CliktError(e.message)
    } catch (e: AdminResponseException) {
        throw CliktError(e.details)
    } catch (e: SerializationException) {
        throw CliktError(e.message)
    } catch (e: VaultException) {
        throw CliktError(e.message)
    } catch (e: ConnectTimeoutException) {
        throw CliktError("${e.message}. Try configuring proxy: vzd-cli global config  ...")
    } catch (e: io.ktor.http.parsing.ParseException) {
        // another InterOp Issue with REST API
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
class Cli : CliktCommand(name = "vzd-cli") {
    init {
        context {
            helpFormatter = CliktHelpFormatter(
                requiredOptionMarker = "*",
                showDefaultValues = true
            )
        }
    }

    private val verbosity by option("-v", help = "Display log, use -vv for even more details").counted()

    init {
        versionOption(BuildConfig.APP_VERSION)
        subcommands(
            GlobalCommand(),
            DirectoryAdministrationCli(),
            ApoCli(),
            GuiCommand(),
            LdifCommand(),
            PersCommand()
        )
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