package de.gematik.ti.directory.cli.admin

import de.gematik.ti.directory.admin.DirectoryEntry
import de.gematik.ti.directory.admin.DirectoryEntryExtSerializer
import de.gematik.ti.directory.cli.escape
import de.gematik.ti.directory.util.ExtendedCertificateDataDERSerializer
import hu.vissy.texttable.dsl.tableFormatter
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import net.mamoe.yamlkt.Yaml

val DirectoryEntryCsvHeaders = listOf(
    "query",
    "telematikID",
    "domainID",
    "holder",
    "displayName",
    "streetAddress",
    "postalCode",
    "localityName",
    "stateOrProvinceName",
    "firstCN",
    "firstIssuer",
    "userCertificateCount",
    "userCertificateSerial",
    "userCertificateOCSPStatus",
    "mailCount",
    "mail",
    "FAD",
    "specialization"
)

var YamlDirectoryEntryExt = Yaml {
    encodeDefaultValues = false
    serializersModule = SerializersModule {
        contextual(ExtendedCertificateDataDERSerializer)
    }
}

var JsonDirectoryEntryExt = Json {
    encodeDefaults = true
    prettyPrint = true
    serializersModule = SerializersModule {
        contextual(ExtendedCertificateDataDERSerializer)
    }
}

val DirectoryEntryOutputFormatters = mapOf(
    OutputFormat.HUMAN to { entry: DirectoryEntry? ->
        entry?.let {
            println(HumanDirectoryYaml.encodeToString(DirectoryEntryHumanSerializer, it))
        }
    },
    OutputFormat.YAML to { entry: DirectoryEntry? -> Output.printYaml(entry) },
    OutputFormat.YAML_EXT to { entry: DirectoryEntry? ->
        entry?.let {
            println(YamlDirectoryEntryExt.encodeToString(DirectoryEntryExtSerializer, it))
        }
    },
    OutputFormat.JSON to { entry: DirectoryEntry? -> Output.printJson(entry) },
    OutputFormat.JSON_EXT to { entry: DirectoryEntry? ->
        entry?.let {
            println(JsonDirectoryEntryExt.encodeToString(DirectoryEntryExtSerializer, it))
        }
    },
)

val DirectoryEntryListOutputFormatters = mapOf(
    OutputFormat.HUMAN to { _: Map<String, String>, value: List<DirectoryEntry>? ->
        value?.let {
            println(HumanDirectoryYaml.encodeToString(ListSerializer(DirectoryEntryHumanSerializer), it))
        }
    },
    OutputFormat.YAML to { _: Map<String, String>, value: List<DirectoryEntry>? -> Output.printYaml(value) },
    OutputFormat.YAML_EXT to { _: Map<String, String>, value: List<DirectoryEntry>? ->
        value?.let {
            println(YamlDirectoryEntryExt.encodeToString(ListSerializer(DirectoryEntryExtSerializer), it))
        }
    },
    OutputFormat.JSON to { _: Map<String, String>, value: List<DirectoryEntry>? -> Output.printJson(value) },
    OutputFormat.JSON_EXT to { _: Map<String, String>, value: List<DirectoryEntry>? ->
        value?.let {
            println(JsonDirectoryEntryExt.encodeToString(ListSerializer(DirectoryEntryExtSerializer), it))
        }
    },
    OutputFormat.SHORT to { _: Map<String, String>, value: List<DirectoryEntry>? ->
        value?.forEach {
            val kims = it.fachdaten?.let { it.mapNotNull { it.fad1 }.map { it.mapNotNull { it.mail } } }
                ?.flatten()?.flatten()?.joinToString() ?: ""
            println(
                "${it.directoryEntryBase.telematikID} ${Json.encodeToString(it.directoryEntryBase.displayName)}" +
                    " ${it.directoryEntryBase.domainID?.joinToString()}" +
                    " $kims"
            )
        }
    },
    OutputFormat.CSV to { query: Map<String, String>, value: List<DirectoryEntry>? ->

        value?.forEach {
            Output.printCsv(
                listOf(
                    query.toString(),
                    it.directoryEntryBase.telematikID.escape(),
                    it.directoryEntryBase.domainID?.joinToString(),
                    it.directoryEntryBase.holder?.joinToString(),
                    it.directoryEntryBase.displayName,
                    it.directoryEntryBase.streetAddress,
                    it.directoryEntryBase.postalCode,
                    it.directoryEntryBase.localityName,
                    it.directoryEntryBase.stateOrProvinceName,
                    it.userCertificates?.mapNotNull { it.userCertificate?.certificateInfo }?.firstOrNull()?.subject ?: "",
                    it.userCertificates?.mapNotNull { it.userCertificate?.certificateInfo }?.firstOrNull()?.issuer ?: "",
                    it.userCertificates?.count { it.userCertificate != null } ?: 0,
                    it.userCertificates?.mapNotNull { it.userCertificate?.certificateInfo }?.map { it.serialNumber }?.joinToString(),
                    it.userCertificates?.mapNotNull { it.userCertificate?.certificateInfo }?.map { it.ocspResponse?.status ?: "NONE" }?.joinToString(),
                    it.fachdaten?.let { it.mapNotNull { it.fad1 }.map { it.mapNotNull { it.mail } } }?.flatten()?.flatten()?.count() ?: 0,
                    it.fachdaten?.let { it.mapNotNull { it.fad1 }.map { it.mapNotNull { it.mail } } }?.flatten()
                        ?.flatten()?.joinToString(),
                    it.fachdaten?.let { it.mapNotNull { it.fad1 }.flatten().mapNotNull { it.dn.ou?.firstOrNull() }.joinToString() },
                    it.directoryEntryBase.specialization?.map { it }?.joinToString() ?: ""

                )
            )
        }

        if (value.isNullOrEmpty()) {
            Output.printCsv(listOf(query.toString(), "Not Found"))
        }
    },

    OutputFormat.TABLE to { _: Map<String, String>, value: List<DirectoryEntry>? ->
        val formatter = tableFormatter<DirectoryEntry> {

            labeled<String>("TelematikID", "Gesamt") {
                extractor { directoryEntry ->
                    directoryEntry.directoryEntryBase.telematikID
                }
            }

            class State(var count: Int = 0)
            stateful<String, State>("Name") {
                initState { State() }
                extractor { directoryEntry, state ->
                    state.count += 1
                    directoryEntry.directoryEntryBase.displayName
                }
                cellFormatter {
                    maxWidth = 24
                }
                aggregator { _, state ->
                    if (state.count > 99) {
                        "99+"
                    } else {
                        state.count.toString()
                    }
                }
            }

            stateless("Address") {
                extractor { directoryEntry ->
                    buildString {
                        append(directoryEntry.directoryEntryBase.streetAddress ?: "n/a")
                        append(" ")
                        append(directoryEntry.directoryEntryBase.postalCode ?: "n/a")
                        append(" ")
                        append(directoryEntry.directoryEntryBase.localityName ?: "n/a")
                    }
                }
                cellFormatter {
                    maxWidth = 40
                }
            }

            showAggregation = true
        }
        println(formatter.apply(value))
    }

)
