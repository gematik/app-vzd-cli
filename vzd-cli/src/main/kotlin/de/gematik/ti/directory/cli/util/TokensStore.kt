/*
 * Copyright (Change Date see Readme), gematik GmbH
 *
 * Licensed under the EUPL, Version 1.2 or - as soon they will be approved by the
 * European Commission – subsequent versions of the EUPL (the "Licence").
 * You may not use this work except in compliance with the Licence.
 *
 * You find a copy of the Licence in the "Licence" file or at
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either expressed or implied.
 * In case of changes by gematik find details in the "Readme" file.
 *
 * See the Licence for the specific language governing permissions and limitations under the Licence.
 *
 * *******
 *
 * For additional notes and disclaimer from gematik and in case of changes by gematik,
 * find details in the "Readme" file.
 */

package de.gematik.ti.directory.cli.util

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonPrimitive
import mu.KotlinLogging
import java.nio.file.Path
import java.util.*
import kotlin.io.path.*

private val logger = KotlinLogging.logger {}

private const val GRACE_PERIOD_MILLIS = -300000

@Serializable
class TokenStoreItem(
    var url: String,
    var accessToken: String
)

class TokenStore(
    customConfigPath: Path? = null
) : FileObjectStore<List<TokenStoreItem>>(
        "directory-tokens.yaml",
        { emptyList() },
        { yaml, stringValue -> yaml.decodeFromString(stringValue) },
        customConfigPath,
    ) {
    fun accessTokenFor(url: String): TokenStoreItem? = value.firstOrNull { it.url == url }

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
        return jsonObject
            .map { entry ->
                if (entry.value is JsonPrimitive) {
                    Pair(entry.key, entry.value.jsonPrimitive.content)
                } else {
                    Pair(entry.key, entry.value.toString())
                }
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
