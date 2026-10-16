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
