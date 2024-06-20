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

fun Bundle.toStringOutput(format: OutputFormat): String {
    return when (format) {
        OutputFormat.JSON -> toJson()
        OutputFormat.JSON_EXT -> toJsonExt()
        OutputFormat.YAML -> toYaml()
        OutputFormat.HUMAN -> toHuman()
        OutputFormat.TABLE -> toTable()
    }
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

fun Bundle.toYaml(): String {
    return toYaml(toJson())
}

fun Bundle.toHuman(): String {
    return toYaml(toJsonExt())
}
