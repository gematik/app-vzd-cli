package de.gematik.ti.directory.cli.admin

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.CliktError
import com.github.ajalt.clikt.core.requireObject
import com.github.ajalt.clikt.output.TermUi
import com.github.ajalt.clikt.parameters.arguments.argument
import de.gematik.ti.directory.admin.BaseDirectoryEntry
import de.gematik.ti.directory.admin.UpdateBaseDirectoryEntry
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import mu.KotlinLogging
import net.mamoe.yamlkt.Yaml

class EditBaseCommand : CliktCommand(name = "edit-base", help = "Edit base entry using text editor") {
    private val logger = KotlinLogging.logger {}
    private val telematikID by argument()
    private val context by requireObject<CommandContext>()

    private val json = Json { ignoreUnknownKeys = true }

    override fun run() = catching {
        val queryResult = runBlocking { context.client.readDirectoryEntry(mapOf("telematikID" to telematikID)) }
        val entry = queryResult?.firstOrNull() ?: throw CliktError("Entry not found for TelematikID: $telematikID")

        TermUi.editText(Yaml.encodeToString(entry.directoryEntryBase), requireSave = true)?.let { edited ->
            val editedBaseDirectoryEntry = Yaml.decodeFromString<BaseDirectoryEntry>(edited)
            val uid = editedBaseDirectoryEntry.dn?.uid ?: throw CliktError("UID ist not specified")
            val jsonData = json.encodeToString(editedBaseDirectoryEntry)
            val updateBaseDirectoryEntry = json.decodeFromString<UpdateBaseDirectoryEntry>(jsonData)

            runBlocking { context.client.modifyDirectoryEntry(uid, updateBaseDirectoryEntry) }

            val result = runBlocking { context.client.readDirectoryEntry(mapOf("uid" to uid)) }

            Output.printYaml(result?.first()?.directoryEntryBase)
        }
    }
}
