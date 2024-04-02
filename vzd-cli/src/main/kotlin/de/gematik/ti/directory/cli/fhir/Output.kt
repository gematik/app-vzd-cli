package de.gematik.ti.directory.cli.fhir

import ca.uhn.fhir.context.FhirContext
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
    HUMAN,
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
        OutputFormat.HUMAN -> toHuman()
    }
}

fun Bundle.toHuman(): String {
    val entries = this.entry.mapNotNull {
        when (it.resource.resourceType) {
            ResourceType.PractitionerRole -> {
                val role = it.resource as PractitionerRole
                ElaboratePractitionerRole(id = role.idElement.idPart).apply(role, this)
            }

            else -> {
                null
            }
        }


    }

    return yamlOutputFormatter.encodeToString(entries)
}
