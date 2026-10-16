/*
Copyright 2026, gematik GmbH

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

*******

For additional notes and disclaimer from gematik and in case of changes
by gematik, find details in the "Readme" file.
*/

package de.gematik.ti.directory.cli.util

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
    private val customPath: Path? = null,
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
