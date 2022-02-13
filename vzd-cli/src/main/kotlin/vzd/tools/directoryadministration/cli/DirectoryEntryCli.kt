package vzd.tools.directoryadministration.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.CliktError
import com.github.ajalt.clikt.core.UsageError
import com.github.ajalt.clikt.core.requireObject
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.associate
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.pair
import com.github.ajalt.clikt.parameters.types.choice
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import mu.KotlinLogging
import net.mamoe.yamlkt.Yaml
import vzd.tools.directoryadministration.*
import java.io.File
import kotlin.io.path.Path
import kotlin.io.path.exists
import kotlin.io.path.useLines
import kotlin.reflect.KMutableProperty
import kotlin.reflect.full.memberProperties
import kotlin.reflect.typeOf

private val logger = KotlinLogging.logger {}

private val CsvHeaders = listOf(
    "query",
    "telematikID",
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

    private fun runQuery(params: Map<String, String>, context: CommandContext) {
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

class CommandTemplate: CliktCommand(name="template", help="""Create template for a resource
     
     Supported types: base, entry, cert
""") {
    private val context by requireObject<CommandContext>()
    val resourceType by argument(help="Specify type of a resource").choice("base", "entry", "cert")

    override fun run() = catching {
        val base = BaseDirectoryEntry(
            telematikID = "1-x.1234567890",
            cn = "Name, Vorname",
            givenName = "Vorname",
            sn = "Nachname",
            displayName = "Name, Vorname",
            streetAddress = "Hauptstraße 1",
            postalCode = "12345",
            countryCode = "DE",
            localityName = "Berlin",
            stateOrProvinceName = "Berlin",
            title = "Dr.",
            domainID = listOf("vzd-cli", "030", "033")
        )
        when (resourceType) {
            "base" -> {
                printTemplate(base, context.outputFormat)
            }
            "entry" -> {
                printTemplate(
                    DirectoryEntry(
                        directoryEntryBase = base,
                        userCertificates = listOf(
                            UserCertificate(
                                userCertificate = CertificateDataDER("BASE64"),
                                description = "Benutzt Zertifikat in DES (CRT) Binärformat konfertiert nach String mittels BASE64"

                            )
                        )
                    ), context.outputFormat)

            }
            "cert" -> {
                printTemplate(UserCertificate(
                    userCertificate = CertificateDataDER("BASE64"),
                    description = "Benutzt Zertifikat in DES (CRT) Binärformat konfertiert nach String mittels BASE64"

                ), context.outputFormat)
            }
            else -> throw UsageError("Undefinded resource type: $resourceType")
        }


    }

    private inline fun <reified T>printTemplate(template: T, outputFormat: OutputFormat) {
        when (outputFormat) {
            OutputFormat.JSON -> Output.printJson(template)
            OutputFormat.HUMAN, OutputFormat.YAML -> Output.printYaml(template)
            else -> throw UsageError("Templates are not available for format: ${context.outputFormat}")
        }
    }
}

class LoadBaseDirectoryEntry: CliktCommand(name = "load-base", help="Load the base entry for editing.") {
    private val params: Map<String, String> by option("-p", "--param",
        help="Specify query parameters to find matching entries").associate()
    private val context by requireObject<CommandContext>()

    private val json = Json {
        prettyPrint = true
        encodeDefaults = true
    }

    private val yaml = Yaml {
        encodeDefaultValues = true
    }

    override fun run() = catching {
        val result = runBlocking {
            context.client.readDirectoryEntry(params)
        }

        if (result?.size != 1) {
            throw UsageError("The query must return exactly one value. Got: ${result?.size}")
        }

        when (context.outputFormat) {
            OutputFormat.JSON -> println ( json.encodeToString(result.first().directoryEntryBase) )
            OutputFormat.HUMAN, OutputFormat.YAML -> println ( yaml.encodeToString(result.first().directoryEntryBase) )
            else -> throw UsageError("Cant load for editing in for format: ${context.outputFormat}")
        }
    }

}

class DeleteDiectoryEntry: CliktCommand(name="delete", help="Delete specified directory entries") {
    private val params: Map<String, String> by option("-p", "--param",
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

class AddBaseDirectoryEntry: CliktCommand(name="add-base", help="Add new directory entry") {
    private val attrs: Map<String, String> by option("-s", "--set", metavar = "ATTR=VALUE",
        help="Set the attribute value in BaseDirectoryEntry.").associate()
    private val file: String? by option("--file", "-f", metavar = "FILENAME",
        help="Read the directory BaseDirectoryEntry from specified file, use - to read data from STDIN")
    private val context by requireObject<CommandContext>()

    override fun run() = catching {
        val data = if (file != null && file == "-") {
            logger.debug { "Loading from STDIN" }
            generateSequence(::readLine).joinToString("\n")
        } else if (file != null) {
            logger.debug { "Loading file: $file" }
            File(file.toString()).readText(Charsets.UTF_8)
        } else {
            null
        }

        val baseDirectoryEntry: BaseDirectoryEntry? = data?.let {
            when(context.outputFormat) {
                OutputFormat.HUMAN, OutputFormat.YAML -> Yaml.decodeFromString(it)
                OutputFormat.JSON -> Json.decodeFromString(it)
                else -> throw CliktError("Unsupported format: ${context.outputFormat}")
            }
        } ?: run {
            val telematikID: String = attrs["telematikID"] ?: throw UsageError("Option --set telematikID=<VALUE> or --file is required")
            BaseDirectoryEntry(
                telematikID = telematikID,
                domainID = listOf("vzd-cli")
            )
        }

        setAttributes(baseDirectoryEntry, attrs)

        logger.debug { "Creating new directory entry with telematikID: ${baseDirectoryEntry?.telematikID}" }

        val dn = runBlocking {  context.client.addDirectoryEntry(CreateDirectoryEntry(baseDirectoryEntry)) }

        logger.info("Created new DirectoryEntry: ${dn.uid}")

        val query = mapOf("uid" to dn.uid)
        val result = runBlocking {  context.client.readDirectoryEntry(query) }

        when (context.outputFormat) {
            OutputFormat.JSON -> Output.printJson(result?.first()?.directoryEntryBase)
            OutputFormat.HUMAN, OutputFormat.YAML -> Output.printYaml(result?.first()?.directoryEntryBase)
            else -> throw UsageError("Cant load for editing in for format: ${context.outputFormat}")
        }
    }
}

class ModifyBaseDirectoryEntry: CliktCommand(name="modify-base", help="Modify single base directory entry") {
    private val params: Map<String, String> by option("-p", "--param",
        help="Specify query parameters to find matching entries").associate()
    private val attrs: Map<String, String> by option("-s", "--set", metavar = "ATTR=VALUE",
        help="Set the attribute value in BaseDirectoryEntry.").associate()
    private val file: String? by option("--file", "-f", metavar = "FILENAME",
        help="Read the directory BaseDirectoryEntry from specified file, use - to read data from STDIN")
    private val context by requireObject<CommandContext>()

    override fun run() = catching {

        val baseFromServer: BaseDirectoryEntry? = params?.let {
            var result = runBlocking { context.client.readDirectoryEntry(params) }
            result?.first()?.let {
                it.directoryEntryBase
            }
        }

        logger.debug { "Data from server: $baseFromServer" }

        val baseFromFile: BaseDirectoryEntry? = file?.let {
            when (it) {
                "-" -> generateSequence(::readLine).joinToString("\n")
                else -> File(file.toString()).readText(Charsets.UTF_8)
            }
        }?.let {
            when(context.outputFormat) {
                OutputFormat.HUMAN, OutputFormat.YAML -> Yaml.decodeFromString(it)
                OutputFormat.JSON -> Json.decodeFromString(it)
                else -> throw CliktError("Unsupported format: ${context.outputFormat}")
            }
        }

        val baseToUpdate = baseFromFile ?: baseFromServer
        val dn = baseFromServer?.dn ?: baseFromFile?.dn

        logger.debug { "Data from file: $baseFromFile" }

        setAttributes(baseToUpdate, attrs)

        logger.debug { "Data will to send to server: $baseToUpdate" }

        if (dn != null && baseToUpdate != null) {
            val jsonData = Json.encodeToString(baseToUpdate)
            val updateBaseDirectoryEntry = Json { ignoreUnknownKeys = true }.decodeFromString<UpdateBaseDirectoryEntry>(jsonData)
            // server bug: when updating telematikID with no certificates the exception is thrown
            updateBaseDirectoryEntry.telematikID = null
            runBlocking { context.client.modifyDirectoryEntry(dn.uid, updateBaseDirectoryEntry) }
            val result = runBlocking {  context.client.readDirectoryEntry(mapOf("uid" to dn.uid)) }

            when (context.outputFormat) {
                OutputFormat.JSON -> Output.printJson(result?.first()?.directoryEntryBase)
                OutputFormat.HUMAN, OutputFormat.YAML -> Output.printYaml(result?.first()?.directoryEntryBase)
                else -> throw UsageError("Cant load for editing in for format: ${context.outputFormat}")
            }
        }


        /*


         */
        //

        /*
        if (file != null) {
            val jsonData = if (file == "-") {
                generateSequence(::readLine).joinToString("\n")
            } else {

            }
            val baseDirectoryEntry = Json.decodeFromString<BaseDirectoryEntry>(jsonData)
            val updateBaseDirectoryEntry = Json { ignoreUnknownKeys = true }.decodeFromString<UpdateBaseDirectoryEntry>(jsonData)
            // arvato bug: when updating telematikID with no certificates the exception is thrown
            //updateBaseDirectoryEntry.telematikID = null
        } else {
            throw UsageError("not implemented yet")
        }
         */
    }
}