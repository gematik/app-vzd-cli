package vzd.tools.directoryadministration.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.UsageError
import com.github.ajalt.clikt.core.requireObject
import com.github.ajalt.clikt.parameters.options.*
import com.github.ajalt.clikt.parameters.types.choice
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import mu.KotlinLogging
import vzd.tools.directoryadministration.*
import java.io.File
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
    private val params: Map<String, String> by option("-Q", "--query",
        help="Specify query parameters to find matching entries").associate()
    private val sync by option(help="use Sync mode").flag()
    private val output by option(help="How the entries should be displayed")
        .choice(*DirectoryEntryOutputMapping.keys.toTypedArray()).default("list")
    private val showRawCert by option("--cert-raw",
        help="Show raw certificate data instead of text summary").flag()
    private val client by requireObject<Client>();

    override fun run() = catching {
        val result: List<DirectoryEntry>? = if (sync) {
            runBlocking {  client.readDirectoryEntryForSync( params ) }
        } else {
            runBlocking {  client.readDirectoryEntry( params ) }
        }

        DirectoryEntryOutputMapping[output]?.invoke(result, showRawCert)
    }

}

class LoadBaseDirectoryEntry: CliktCommand(name = "load-base", help="Load the base entry for editing.") {
    private val params: Map<String, String> by option("-Q", "--query",
        help="Specify query parameters to find matching entries").associate()
    private val sync by option(help="use Sync mode").flag()
    private val client by requireObject<Client>();

    override fun run() = catching {
        val result: List<DirectoryEntry>?
        if (sync) {
            result = runBlocking {  client.readDirectoryEntryForSync( params ) }
        } else {
            result = runBlocking {  client.readDirectoryEntry( params ) }
        }

        if (result?.size != 1) {
            throw UsageError("The query must return exactly one value. Got: ${result?.size}")
        }

        echo( Json { prettyPrint=true }.encodeToString(result.first().directoryEntryBase))
    }

}

class DeleteDiectoryEntry: CliktCommand(name="delete", help="Delete specified directory entries") {
    private val params: Map<String, String> by option("-Q", "--query",
        help="Specify query parameters to find matching entries").associate()
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
    private val attrs: Map<String, String> by option("-s", "--set", metavar = "ATTR=VALUE",
        help="Set the attribute value in BaseDirectoryEntry.").associate()
    private val file by option("--file", "-f", metavar = "FILENAME.json",
        help="Read the directory entry from specified JSON file")
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

class ModifyBaseDirectoryEntry: CliktCommand(name="modify-base", help="Modify base directory entry") {
    private val params: Map<String, String> by option("-Q", "--query", metavar = "PARAM=VALUE",
        help="Specify query parameters to find matching entries").associate()
    private val attrs: Map<String, String> by option("-s", "--set", metavar = "ATTR=VALUE",
        help="Set the attribute value in BaseDirectoryEntry.").associate()
    private val file by option("--file", "-f",
        help="Read base directory entry from file.")
    private val client by requireObject<Client>();

    override fun run() {
        if (file != null) {
            var jsonData = File(file).readText(Charsets.UTF_8)
            val baseDirectoryEntry = Json.decodeFromString<BaseDirectoryEntry>(jsonData)
            val updateBaseDirectoryEntry = Json { ignoreUnknownKeys = true }.decodeFromString<UpdateBaseDirectoryEntry>(jsonData)
            // arvato bug: when updating telematikID with no certificates the exception is thrown
            updateBaseDirectoryEntry.telematikID = null
            runBlocking { client.modifyDirectoryEntry(baseDirectoryEntry.dn!!.uid, updateBaseDirectoryEntry) }
        } else {
            throw UsageError("not implemented yet")
        }


    }
}