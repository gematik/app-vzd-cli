package de.gematik.ti.directory.cli.fhir

import ca.uhn.fhir.context.FhirContext
import de.gematik.ti.directory.pki.ExtendedCertificateDataDERSerializer
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import net.mamoe.yamlkt.Yaml
import org.hl7.fhir.r4.model.Bundle


val yamlOutputFormatter =
    Yaml {
        encodeDefaultValues = false
    }

val jsonOutputFormatter = Json {
    encodeDefaults = true
    prettyPrint = true
    serializersModule =
        SerializersModule {
            contextual(ExtendedCertificateDataDERSerializer)
        }
}

enum class OutputFormat {
    JSON,
    YAML,
}

var ctx = FhirContext.forR4()

fun Bundle.toPrettyJson(): String {
    val parser = ctx.newJsonParser()
    parser.setPrettyPrint(true)
    return parser.encodeResourceToString(this)
}

fun Bundle.toStringOutput(format: OutputFormat): String {
    return when (format) {
        OutputFormat.JSON -> jsonOutputFormatter.encodeToString(this)
        OutputFormat.YAML -> yamlOutputFormatter.encodeToString(this)
    }
}