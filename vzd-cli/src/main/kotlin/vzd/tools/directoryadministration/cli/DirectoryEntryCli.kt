package vzd.tools.directoryadministration.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.CliktError
import com.github.ajalt.clikt.core.UsageError
import com.github.ajalt.clikt.core.requireObject
import com.github.ajalt.clikt.parameters.options.associate
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.pair
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import mu.KotlinLogging
import vzd.tools.directoryadministration.BaseDirectoryEntry
import vzd.tools.directoryadministration.CreateDirectoryEntry
import vzd.tools.directoryadministration.DirectoryEntry
import vzd.tools.directoryadministration.UpdateBaseDirectoryEntry
import java.io.File
import kotlin.io.path.Path
import kotlin.io.path.exists
import kotlin.io.path.useLines
import kotlin.reflect.KMutableProperty
import kotlin.reflect.full.memberProperties
import kotlin.reflect.typeOf

private val logger = KotlinLogging.logger {}

val CsvHeaders = listOf(
    "Query",
    "TelematikID",
    "displayName",
    "streetAddress",
    "postalCode",
    "localityName",
    "stateOrProvinceName",
    "certificateCount",
)

val DirectoryEntryOutputMapping = mapOf(
    OutputFormat.HUMAN to { _: Map<String, String>, value: List<DirectoryEntry>? -> Output.printHuman(value) },
    OutputFormat.YAML to { _: Map<String, String>, value: List<DirectoryEntry>? -> Output.printYaml(value) },
    OutputFormat.JSON to { _: Map<String, String>, value: List<DirectoryEntry>? -> Output.printJson(value) },
    OutputFormat.SHORT to { _: Map<String, String>, value: List<DirectoryEntry>? ->
        value?.forEach {
            println("${it.directoryEntryBase.dn?.uid} ${it.directoryEntryBase.telematikID} ${Json.encodeToString(it.directoryEntryBase.displayName)}")
        }
    },
    OutputFormat.CSV to {query: Map<String, String>, value: List<DirectoryEntry>? ->

        value?.forEach {
            Output.printCsv(listOf(
                query.toString(),
                it.directoryEntryBase.telematikID,
                it.directoryEntryBase.displayName,
                it.directoryEntryBase.streetAddress,
                it.directoryEntryBase.postalCode,
                it.directoryEntryBase.localityName,
                it.directoryEntryBase.stateOrProvinceName,
                it.userCertificates?.size.toString(),
            ))
        }

        if (value == null || value.isEmpty()) {
            Output.printCsv(listOf(query.toString(), "Not Found"))
        }

    },
)

class ListDirectoryEntries: CliktCommand(name = "list", help="List directory entries") {
    private val paramFile: Pair<String, String>? by option("-f", "--param-file",
        help="Read parameter values from file", metavar = "PARAM FILENAME").pair()
    private val params: Map<String, String> by option("-p", "--param",
        help="Specify query parameters to find matching entries").associate()
    private val context by requireObject<CommandContext>()

    override fun run() = catching {
        paramFile?.let { paramFile ->
            val file = Path(paramFile.second)
            if (!file.exists()) throw CliktError("File not found: ${paramFile.second}")
            file.useLines { line ->
                line.forEach {
                    runQuery(params + Pair(paramFile.first, it), context)
                }
            }
        } ?: run {
            runQuery(params, context)
        }

    }

    fun runQuery(params: Map<String, String>, context: CommandContext) {
        val result: List<DirectoryEntry>? = if (context.syncMode) {
            runBlocking {  context.client.readDirectoryEntryForSync( params ) }
        } else {
            runBlocking {  context.client.readDirectoryEntry( params ) }
        }

        if (context.outputFormat == OutputFormat.CSV) {
            if (context.firstCommand) {
                context.firstCommand = false
                Output.printCsv(CsvHeaders)
            }
        }

        DirectoryEntryOutputMapping[context.outputFormat]?.invoke(params, result)
    }

}

class LoadBaseDirectoryEntry: CliktCommand(name = "load-base", help="Load the base entry for editing.") {
    private val params: Map<String, String> by option("-p", "--param",
        help="Specify query parameters to find matching entries").associate()
    private val sync by option(help="use Sync mode").flag()
    private val context by requireObject<CommandContext>()

    override fun run() = catching {
        val result = runBlocking {
            if (sync) {
                context.client.readDirectoryEntryForSync(params)
            } else {
                context.client.readDirectoryEntry(params)
            }
        }

        if (result?.size != 1) {
            throw UsageError("The query must return exactly one value. Got: ${result?.size}")
        }

        Output.printJson(result.first().directoryEntryBase)
    }

}

class DeleteDiectoryEntry: CliktCommand(name="delete", help="Delete specified directory entries") {
    private val params: Map<String, String> by option("-Q", "--query",
        help="Specify query parameters to find matching entries").associate()
    //val force by option(help="Force delete").flag()
    private val context by requireObject<CommandContext>()

    override fun run() = catching {
        runBlocking {
            if (params.isEmpty()) {
                throw UsageError("Specify at least one query parameter")
            }
            val result = context.client.readDirectoryEntry(params)
            result?.forEach {
                val answer = prompt("Type YES to delete '${it.directoryEntryBase.displayName}' '${it.directoryEntryBase.dn?.uid}'")
                if (answer == "YES") {
                    logger.debug { "Deleting '${it.directoryEntryBase.displayName}' '${it.directoryEntryBase.dn?.uid}'" }
                    if (it.directoryEntryBase.dn?.uid != null) {
                        context.client.deleteDirectoryEntry( it.directoryEntryBase.dn!!.uid )
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
    private val file: String? by option("--file", "-f", metavar = "FILENAME.json",
        help="Read the directory entry from specified JSON file, use - to read data from STDIN")
    private val context by requireObject<CommandContext>()

    override fun run() = catching {

        val directoryEntry = if (file != null && file == "-") {
            logger.debug { "Loading from STDIN" }
            Json.decodeFromString(generateSequence(::readLine).joinToString("\n"))
        } else if (file != null) {
            logger.debug { "Loading file: $file" }
            Json.decodeFromString(File(file.toString()).readText(Charsets.UTF_8))
        } else {
            val telematikID: String = attrs["telematikID"] ?: throw UsageError("Option --set telematikID or --file is required")
            val baseDirectoryEntry = BaseDirectoryEntry(
                telematikID = telematikID,
                domainID = listOf("vzd-cli")
            )
            CreateDirectoryEntry(baseDirectoryEntry)
        }

        setAttributes(directoryEntry.directoryEntryBase, attrs)

        logger.debug { "Creating new directory entry with telematikID: ${directoryEntry.directoryEntryBase?.telematikID}" }

        val dn = runBlocking {  context.client.addDirectoryEntry(directoryEntry) }
        echo(dn.uid)
    }
}

class ModifyBaseDirectoryEntry: CliktCommand(name="modify-base", help="Modify base directory entry") {
    private val query: Map<String, String> by option("-q", "--query", metavar = "PARAM=VALUE",
        help="Specify query parameters to find matching entries").associate()
    private val attrs: Map<String, String> by option("-s", "--set", metavar = "ATTR=VALUE",
        help="Set the attribute value in BaseDirectoryEntry.").associate()
    private val file: String? by option("--file", "-f",
        help="Read base directory entry from file. Use - to read data from STDIN.")
    private val context by requireObject<CommandContext>()

    override fun run() = catching {
        if (file != null) {
            val jsonData = if (file == "-") {
                generateSequence(::readLine).joinToString("\n")
            } else {
                File(file.toString()).readText(Charsets.UTF_8)
            }
            val baseDirectoryEntry = Json.decodeFromString<BaseDirectoryEntry>(jsonData)
            val updateBaseDirectoryEntry = Json { ignoreUnknownKeys = true }.decodeFromString<UpdateBaseDirectoryEntry>(jsonData)
            // arvato bug: when updating telematikID with no certificates the exception is thrown
            //updateBaseDirectoryEntry.telematikID = null
            runBlocking { context.client.modifyDirectoryEntry(baseDirectoryEntry.dn!!.uid, updateBaseDirectoryEntry) }
        } else {
            throw UsageError("not implemented yet")
        }
    }
}