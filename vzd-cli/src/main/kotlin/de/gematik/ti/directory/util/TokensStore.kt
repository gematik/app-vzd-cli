package de.gematik.ti.directory.util

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import mu.KotlinLogging
import net.mamoe.yamlkt.Yaml
import java.nio.file.Path
import java.util.*
import kotlin.io.path.*

private val logger = KotlinLogging.logger {}

private const val GRACE_PERIOS_MILLIS = -300000

@Serializable
class TokenStoreItem(var url: String, var accessToken: String)

class TokenStore(private val customPath: Path? = null) {

    private val defaultPath = Path(System.getProperty("user.home"), ".telematik", "directory-tokens.yaml")
    private val path get() = customPath ?: defaultPath
    private var items: List<TokenStoreItem>

    init {
        items = if (!path.toFile().exists()) {
            path.absolute().parent.toFile().mkdirs()
            listOf()
        } else {
            Yaml.decodeFromString(path.readText())
        }
    }

    fun exists(): Boolean {
        return path.exists()
    }

    fun purge() {
        logger.debug { "Purging the tokens store: $path" }
        path.deleteIfExists()
    }

    fun accessTokenFor(url: String): TokenStoreItem? {
        return items.firstOrNull() { it.url == url }
    }

    fun addAccessToken(url: String, accessToken: String) {
        val item = TokenStoreItem(url, accessToken)
        items = items.filter { it.url != url } + item
        save()
    }

    fun claimsFor(url: String): JsonObject? {
        accessTokenFor(url)?.let {  item ->
            return tokenToClaims(item.accessToken)
        }
        return null
    }

    private fun tokenToClaims(token: String): JsonObject {
        val tokenParts = token.split(".")
        val tokenBody = String(Base64.getUrlDecoder().decode(tokenParts[1]), Charsets.UTF_8)
        return Json.decodeFromString(tokenBody)
    }

    private fun save() {
        path.writeText(Yaml.encodeToString(items))
    }

    fun removeExpired() {
        val now = Date().time
        val validItems = items.filter { item ->
            try {
                val claims = tokenToClaims(item.accessToken)
                claims["exp"]?.jsonPrimitive?.long?.let {
                    (it*1000)+GRACE_PERIOS_MILLIS >= now
                } ?: false
            } catch (e: Exception) {
                false
            }
        }

        if (validItems != items) {
            logger.debug { "Some tokens have expired, updating the token store" }
            items = validItems
            save()
        }

    }
}
