package vzd.tools.directoryadministration.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.UsageError
import com.github.ajalt.clikt.core.requireObject
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.multiple
import com.github.ajalt.clikt.parameters.options.associate
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.choice
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import mu.KotlinLogging
import net.mamoe.yamlkt.Yaml
import vzd.tools.directoryadministration.Client
import vzd.tools.directoryadministration.DirectoryEntry
import vzd.tools.directoryadministration.UserCertificate

private val logger = KotlinLogging.logger {}

val DirectoryEntryOutputMapping = mapOf(
    "yaml" to { value: List<DirectoryEntry>?, showRawCert: Boolean -> printYaml(value, showRawCert) },
    "json" to { value: List<DirectoryEntry>?, showRawCert: Boolean -> printJson(value, showRawCert) },
    "list" to { value: List<DirectoryEntry>?, _: Boolean ->
        value?.forEach {
            println("${it.directoryEntryBase.dn?.uid} ${it.directoryEntryBase.telematikID} ${Json.encodeToString(it.directoryEntryBase.displayName)}")
        }
    },
)

class ListDirectoryEntries: CliktCommand(name = "list", help="List directory entries") {
    private val params: Map<String, String> by option("-Q", "--query").associate()
    private val sync by option(help="use Sync mode").flag()
    private val output by option(help="How the entries should be displayed").choice(*DirectoryEntryOutputMapping.keys.toTypedArray()).default("list")
    private val showRawCert by option("--cert-raw", help="Show raw certificate data instead of text summary").flag()
    private val client by requireObject<Client>();
    override fun run() {
        val result: List<DirectoryEntry>?
        if (sync) {
            result = runBlocking {  client.readDirectoryEntryForSync( params ) }
        } else {
            result = runBlocking {  client.readDirectoryEntry( params ) }
        }

        DirectoryEntryOutputMapping[output]?.invoke(result, showRawCert)
    }

}

class DeleteDiectoryEntry: CliktCommand(name="delete", help="Delete specified directory entries") {
    val uid by argument(help="List of UIDs for to be deleted directory entries").multiple(required = true)
    val force by option(help="Force delete").flag()
    private val client by requireObject<Client>()

    override fun run() {
        if (force) {
            logger.debug { "Deleting {uid}" }
            runBlocking {
                uid.forEach { client.deleteDirectoryEntry( it ) }
            }
        } else {
            throw UsageError("Specify --force option")
        }

    }
}

class AddDirectoryEntry: CliktCommand(name="add", help="Add new directory entry") {
    override fun run() {
        TODO("Not yet implemented")
    }
}

class ModifyDirectoryEntry: CliktCommand(name="modify", help="Modify directory entry") {
    override fun run() {
        TODO("Not yet implemented")
    }
}