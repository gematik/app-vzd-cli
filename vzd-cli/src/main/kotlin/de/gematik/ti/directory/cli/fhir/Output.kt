package de.gematik.ti.directory.cli.fhir

import ca.uhn.fhir.context.FhirContext
import de.gematik.ti.directory.fhir.toDirectoryEntries
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import net.mamoe.yamlkt.Yaml
import org.hl7.fhir.r4.model.*

val yamlOutputFormatter =
    Yaml {
        encodeDefaultValues = false
        serializersModule = FHIRSerializerModule
    }

val jsonOutputFormatter =
    Json {
        encodeDefaults = true
        prettyPrint = true
        serializersModule = FHIRSerializerModule
    }

enum class OutputFormat {
    JSON,
    JSON_EXT,
    YAML_EXT,
    TABLE,
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
        OutputFormat.TABLE -> toTable()
    }
}

fun Bundle.toYamlExt(): String {
    val entries = this.toDirectoryEntries()
    return yamlOutputFormatter.encodeToString(ListSerializer(FHIRDirectoryEntrySerializer), entries)
}

fun Bundle.toJsonExt(): String {
    val entries = this.toDirectoryEntries()
    return jsonOutputFormatter.encodeToString(ListSerializer(FHIRDirectoryEntrySerializer), entries)
}
