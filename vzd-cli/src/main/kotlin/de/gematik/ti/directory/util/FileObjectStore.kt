package de.gematik.ti.directory.util

import mu.KotlinLogging
import net.mamoe.yamlkt.Yaml
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.readText
import kotlin.io.path.writeText

private val logger = KotlinLogging.logger {}
private val YAML = Yaml { encodeDefaultValues = false }

/**
 * Stores arbitrary object in file
 */
open class FileObjectStore<T>(
    val filename: String,
    private val defaultValue: (() -> T),
    val deserialize: ((yaml: Yaml, stringValue: String) -> T),
    private val customPath: Path? = null
) {
    private val defaultPath = Path(System.getProperty("user.home"), ".telematik", filename)
    val path get() = customPath ?: defaultPath
    var value: T

    init {
        if (!path.toFile().exists()) {
            path.parent.toFile().mkdirs()
            value = defaultValue.invoke()
            save()
        } else {
            try {
                value = deserialize.invoke(YAML, path.readText())
            } catch (e: Exception) {
                logger.error { "Error loading the YAML file" }
                logger.error { e }
                value = defaultValue()
            }
        }
    }

    fun reset(): T {
        value = defaultValue.invoke()
        save()
        return value
    }

    fun save() {
        path.writeText(YAML.encodeToString(value))
    }
}
