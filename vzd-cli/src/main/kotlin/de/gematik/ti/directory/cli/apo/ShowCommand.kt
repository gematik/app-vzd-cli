package de.gematik.ti.directory.cli.apo

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.requireObject
import com.github.ajalt.clikt.parameters.arguments.argument
import de.gematik.ti.directory.apo.ApoCliContext
import de.gematik.ti.directory.cli.catching
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject

private val JSON = Json {
    prettyPrint = true
}

class ShowCommand : CliktCommand(name = "show", help = "Show all information about an entry") {
    private val context by requireObject<ApoInstanceCliContext>()
    private val id by argument()

    override fun run() = catching {
        val jsonString = runBlocking { context.client.getLocationByTelamatikID(id) }
        val jsonObject = JSON.decodeFromString<JsonObject>(jsonString.first)
        echo(JSON.encodeToString(jsonObject["entry"]?.jsonArray?.first()?.jsonObject?.get("resource")))
    }
}
