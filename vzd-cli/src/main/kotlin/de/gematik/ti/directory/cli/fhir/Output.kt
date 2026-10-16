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
