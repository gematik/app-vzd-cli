package de.gematik.ti.directory.cli.util

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import mu.KotlinLogging
import java.nio.file.Path
import java.util.*
import kotlin.io.path.*

private val logger = KotlinLogging.logger {}

private const val GRACE_PERIOD_MILLIS = -300000

@Serializable
class TokenStoreItem(var url: String, var accessToken: String)

class TokenStore(customConfigPath: Path? = null) : FileObjectStore<List<TokenStoreItem>>(
    "directory-tokens.yaml",
    { emptyList() },
    { yaml, stringValue -> yaml.decodeFromString(stringValue) },
    customConfigPath,
) {
    fun accessTokenFor(url: String): TokenStoreItem? {
        return value.firstOrNull { it.url == url }
    }

    fun addAccessToken(
        url: String,
        accessToken: String,
    ) {
        val item = TokenStoreItem(url, accessToken)
        value = value.filter { it.url != url } + item
        save()
    }

    fun claimsFor(url: String): Map<String, String>? {
        accessTokenFor(url)?.let { item ->
            return tokenToClaims(item.accessToken)
        }
        return null
    }

    private fun tokenToClaims(token: String): Map<String, String> {
        val tokenParts = token.split(".")
        val tokenBody = String(Base64.getUrlDecoder().decode(tokenParts[1]), Charsets.UTF_8)
        val jsonObject = Json.decodeFromString<JsonObject>(tokenBody)
        return jsonObject.map { entry ->
            Pair(entry.key, entry.value.jsonPrimitive.content)
        }.toMap()
    }

    fun removeExpired() {
        val now = Date().time
        val validItems =
            value.filter { item ->
                try {
                    val claims = tokenToClaims(item.accessToken)
                    claims["exp"]?.toLong()?.let {
                        (it * 1000) + GRACE_PERIOD_MILLIS >= now
                    } ?: false
                } catch (e: Exception) {
                    false
                }
            }

        if (validItems != value) {
            logger.debug { "Some tokens have expired, updating the token store" }
            value = validItems
            save()
        }
    }
}
