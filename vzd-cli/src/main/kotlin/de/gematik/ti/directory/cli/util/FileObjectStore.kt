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
