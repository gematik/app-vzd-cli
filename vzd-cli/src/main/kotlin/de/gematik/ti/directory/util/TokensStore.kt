package de.gematik.ti.directory.util

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import mu.KotlinLogging
import net.mamoe.yamlkt.Yaml
import java.nio.file.Path
import kotlin.io.path.*

private val logger = KotlinLogging.logger {}

@Serializable
private class TokenStoreItem(var url: String, var accessToken: String)

class TokenStore(val customPath: Path? = null) {

    private val defaultPath = Path(System.getProperty("user.home"), ".telematik", "directory-tokens.yaml")
    private val path get() = customPath ?: defaultPath
    private var items: List<TokenStoreItem>

    init {
        if (!path.toFile().exists()) {
            path.absolute().parent.toFile().mkdirs()
            items = mutableListOf()
        } else {
            items = Yaml.decodeFromString(path.readText())
        }
    }

    fun exists(): Boolean {
        return path.exists()
    }

    fun purge() {
        logger.debug { "Purging the tokens store: $path" }
        path.deleteIfExists()
    }

    fun accessTokenFor(url: String): String? {
        return items.firstOrNull() { it.url == url }?.accessToken
    }

    fun addAccessToken(url: String, accessToken: String) {
        val item = TokenStoreItem(url, accessToken)
        items = items.filter { it.url != url } + item
        path.writeText(Yaml.encodeToString(items))
    }
}
