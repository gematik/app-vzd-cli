package vzd.tools.directoryadministration.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.UsageError
import com.github.ajalt.clikt.core.requireObject
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.multiple
import com.github.ajalt.clikt.parameters.options.*
import com.github.ajalt.clikt.parameters.types.choice
import io.github.cdimascio.dotenv.dotenv
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import mu.KotlinLogging
import net.mamoe.yamlkt.Yaml
import vzd.tools.directoryadministration.*
import java.io.File
import java.nio.charset.Charset
import kotlin.math.log
import kotlin.reflect.KMutableProperty
import kotlin.reflect.full.memberProperties
import kotlin.reflect.typeOf

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

class LoadDirectoryEntry: CliktCommand(name = "load", help="Load the specified entry") {
    private val params: Map<String, String> by option("-Q", "--query").associate()
    private val sync by option(help="use Sync mode").flag()
    private val client by requireObject<Client>();
    override fun run() {
        val result: List<DirectoryEntry>?
        if (sync) {
            result = runBlocking {  client.readDirectoryEntryForSync( params ) }
        } else {
            result = runBlocking {  client.readDirectoryEntry( params ) }
        }

        if (result?.size != 1) {
            throw UsageError("The query must return exactly one value. Got: ${result?.size}")
        }

        echo( Json { prettyPrint=true }.encodeToString(result.first()))
    }

}

class DeleteDiectoryEntry: CliktCommand(name="delete", help="Delete specified directory entries") {
    private val params: Map<String, String> by option("-Q", "--query").associate()
    //val force by option(help="Force delete").flag()
    private val client by requireObject<Client>()

    override fun run() {
        runBlocking {
            if (params.isEmpty()) {
                throw UsageError("Specify at least one query parameter")
            }
            val result = client.readDirectoryEntry(params)
            result?.forEach {
                val answer = prompt("Type YES to delete '${it.directoryEntryBase.displayName}' '${it.directoryEntryBase.dn?.uid}'")
                if (answer == "YES") {
                    logger.debug { "Deleting '${it.directoryEntryBase.displayName}' '${it.directoryEntryBase.dn?.uid}'" }
                    if (it.directoryEntryBase.dn?.uid != null) {
                        client.deleteDirectoryEntry( it.directoryEntryBase.dn!!.uid )
                    }
                }
            }
        }
        /*
        if (force) {
            logger.debug { "Deleting {uid}" }
            runBlocking {

            }
        } else {
            throw UsageError("Specify --force option")
        }*/

    }
}

private fun setAttributes(baseDirectoryEntry: BaseDirectoryEntry?, attrs: Map<String, String>) {
    attrs.forEach { (name, value) ->

        val property = BaseDirectoryEntry::class.memberProperties
            .filterIsInstance<KMutableProperty<*>>()
            .first { it.name == name }

        if (property.returnType == typeOf<String>() || property.returnType == typeOf<String?>()) {
            property.setter.call(baseDirectoryEntry, value)
        } else if (property.returnType == typeOf<List<String>>() || property.returnType == typeOf<List<String>?>()) {
            property.setter.call(baseDirectoryEntry, value.split(',').map { it.trim() })
        } else {
            throw UsageError("Unsupported property type '$name': ${property.returnType}")
        }
    }

}

class AddDirectoryEntry: CliktCommand(name="add", help="Add new directory entry") {
    private val attrs: Map<String, String> by option("-s", "--set", metavar = "ATTR=VALUE", help="Set the attribute value in BaseDirectoryEntry.").associate()
    private val file by option("--file", "-f")

    private val client by requireObject<Client>();
    override fun run() {


        val directoryEntry: CreateDirectoryEntry;

        if (file != null) {
            logger.debug { "Loading file: $file" }
            directoryEntry = Json.decodeFromString<CreateDirectoryEntry>(File(file).readText(Charsets.UTF_8))
        } else {
            val telematikID: String = attrs.get("telematikID") ?: throw UsageError("Attribute telematikID is required")
            val baseDirectoryEntry = BaseDirectoryEntry(
                telematikID = telematikID,
                domainID = listOf("vzd-cli")
            )
            directoryEntry = CreateDirectoryEntry(baseDirectoryEntry)
        }


        setAttributes(directoryEntry.directoryEntryBase, attrs)

        logger.debug { "Creating new directory entry with telematikID: ${directoryEntry.directoryEntryBase?.telematikID}" }
        logger.debug(Json { prettyPrint = true }.encodeToString(directoryEntry))

        val dn = runBlocking {  client.addDirectoryEntry(directoryEntry) }
        echo(dn.uid)
    }
}

class ModifyDirectoryEntry: CliktCommand(name="modify", help="Modify directory entry") {
    override fun run() {
        TODO("Not yet implemented")
    }
}