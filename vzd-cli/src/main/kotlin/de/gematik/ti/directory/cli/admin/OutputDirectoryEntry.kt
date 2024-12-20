package de.gematik.ti.directory.cli.admin

import de.gematik.ti.directory.admin.DirectoryEntry
import de.gematik.ti.directory.cli.toJsonPretty
import de.gematik.ti.directory.cli.toYamlNoDefaults
import de.gematik.ti.directory.elaborate.elaborate
import de.gematik.ti.directory.pki.ExtendedCertificateDataDERSerializer
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import net.mamoe.yamlkt.Yaml

var yamlDirectoryEntryExt =
    Yaml {
        encodeDefaultValues = false
        serializersModule =
            SerializersModule {
                contextual(ExtendedCertificateDataDERSerializer)
            }
    }

fun DirectoryEntry.toYamlExt(): String = yamlDirectoryEntryExt.encodeToString(this)

fun List<DirectoryEntry>.toYamlExt(): String = yamlDirectoryEntryExt.encodeToString(this)

var jsonDirectoryEntryExt =
    Json {
        encodeDefaults = true
        prettyPrint = true
        serializersModule =
            SerializersModule {
                contextual(ExtendedCertificateDataDERSerializer)
            }
    }

fun DirectoryEntry.toJsonExt(): String = jsonDirectoryEntryExt.encodeToString(this.elaborate())

fun List<DirectoryEntry>.toJsonExt(): String = jsonDirectoryEntryExt.encodeToString(this.map { it.elaborate() })

fun DirectoryEntry.toStringRepresentation(format: RepresentationFormat): String =
    when (format) {
        RepresentationFormat.HUMAN -> this.toHuman()
        RepresentationFormat.YAML -> this.toYamlNoDefaults()
        RepresentationFormat.YAML_EXT -> this.toYamlExt()
        RepresentationFormat.JSON -> this.toJsonPretty()
        RepresentationFormat.JSON_EXT -> this.toJsonExt()
        else -> ""
    }

fun List<DirectoryEntry>.toStringRepresentation(format: RepresentationFormat): String =
    when (format) {
        RepresentationFormat.HUMAN -> this.toHuman()
        RepresentationFormat.YAML -> this.toYamlNoDefaults()
        RepresentationFormat.YAML_EXT -> this.toYamlExt()
        RepresentationFormat.JSON -> this.toJsonPretty()
        RepresentationFormat.JSON_EXT -> this.toJsonExt()
        RepresentationFormat.TABLE -> this.toTable()
        RepresentationFormat.CSV -> this.toCsv()
    }
