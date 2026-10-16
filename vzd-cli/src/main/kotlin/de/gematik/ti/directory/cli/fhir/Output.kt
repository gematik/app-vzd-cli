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

package de.gematik.ti.directory.cli.fhir

import com.fasterxml.jackson.core.JsonFactory
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import de.gematik.ti.directory.fhir.FHIRSerializerModule
import de.gematik.ti.directory.fhir.FhirContextR4
import de.gematik.ti.directory.fhir.toDirectoryEntries
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.hl7.fhir.r4.model.*

val fhirExtFormatter =
    Json {
        prettyPrint = true
        serializersModule = FHIRSerializerModule
    }

enum class OutputFormat {
    JSON,
    JSON_EXT,
    YAML,
    HUMAN,
    TABLE,
}

fun Bundle.toJson(): String {
    val parser = FhirContextR4.newJsonParser()
    parser.setPrettyPrint(true)
    return parser.encodeResourceToString(this)
}

fun Bundle.toStringOutput(format: OutputFormat): String =
    when (format) {
        OutputFormat.JSON -> toJson()
        OutputFormat.JSON_EXT -> toJsonExt()
        OutputFormat.YAML -> toYaml()
        OutputFormat.HUMAN -> toHuman()
        OutputFormat.TABLE -> toTable()
    }

fun Bundle.toJsonExt(): String {
    val entries = this.toDirectoryEntries()
    return fhirExtFormatter.encodeToString(entries)
}

private fun toYaml(jsonString: String): String {
    // parse json
    val jsonMapper = ObjectMapper(JsonFactory())
    val json = jsonMapper.readTree(jsonString)
    // convert to yaml
    val yamlMapper = ObjectMapper(YAMLFactory())
    return yamlMapper.writeValueAsString(json)
}

fun Bundle.toYaml(): String = toYaml(toJson())

fun Bundle.toHuman(): String = toYaml(toJsonExt())
