package de.gematik.ti.directory.cli.fhir

import ca.uhn.fhir.context.FhirContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import net.mamoe.yamlkt.Yaml
import org.hl7.fhir.r4.model.*


val yamlOutputFormatter =
    Yaml {
        encodeDefaultValues = false
        serializersModule = FHIRSerializerModule
    }

val jsonOutputFormatter = Json {
    encodeDefaults = true
    prettyPrint = true
    serializersModule = FHIRSerializerModule
}

enum class OutputFormat {
    JSON,
    JSON_EXT,
    YAML_EXT,
}

var ctx = FhirContext.forR4()

fun Bundle.toPrettyJson(): String {
    val parser = ctx.newJsonParser()
    parser.setPrettyPrint(true)
    return parser.encodeResourceToString(this)
}

fun Bundle.toStringOutput(format: OutputFormat): String {
    return when (format) {
        OutputFormat.JSON -> toPrettyJson()
        OutputFormat.JSON_EXT -> toJsonExt()
        OutputFormat.YAML_EXT -> toYamlExt()
    }
}

fun Bundle.toYamlExt(): String {
    val elaborateBundle = elaborateBundle()
    return yamlOutputFormatter.encodeToString(elaborateBundle)
}

fun Bundle.toJsonExt(): String {
    val elaborateBundle = elaborateBundle()
    return jsonOutputFormatter.encodeToString(elaborateBundle)
}
