package vzd.admin.cli

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import vzd.admin.client.DirectoryEntry

val DirectoryEntryCsvHeaders = listOf(
    "query",
    "telematikID",
    "displayName",
    "streetAddress",
    "postalCode",
    "localityName",
    "stateOrProvinceName",
    "certificateCount",
    "kimAdresses"
)

val DirectoryEntryOutputMapping = mapOf(
    OutputFormat.HUMAN to { _: Map<String, String>, value: List<DirectoryEntry>? -> Output.printHuman(value) },
    OutputFormat.YAML to { _: Map<String, String>, value: List<DirectoryEntry>? -> Output.printYaml(value) },
    OutputFormat.JSON to { _: Map<String, String>, value: List<DirectoryEntry>? -> Output.printJson(value) },
    OutputFormat.SHORT to { _: Map<String, String>, value: List<DirectoryEntry>? ->
        value?.forEach {
            println("${it.directoryEntryBase.dn?.uid} ${it.directoryEntryBase.telematikID} ${Json.encodeToString(it.directoryEntryBase.displayName)}")
        }
    },
    OutputFormat.CSV to { query: Map<String, String>, value: List<DirectoryEntry>? ->

        value?.forEach {
            Output.printCsv(listOf(
                query.toString(),
                it.directoryEntryBase.telematikID.escape(),
                it.directoryEntryBase.displayName,
                it.directoryEntryBase.streetAddress,
                it.directoryEntryBase.postalCode,
                it.directoryEntryBase.localityName,
                it.directoryEntryBase.stateOrProvinceName,
                it.userCertificates?.size.toString(),
                it.fachdaten?.let { it.mapNotNull { it.fad1 }.mapNotNull { it.mapNotNull { it.mail } } }?.flatten()
                    ?.flatten()?.joinToString()
            ))
        }

        if (value == null || value.isEmpty()) {
            Output.printCsv(listOf(query.toString(), "Not Found"))
        }

    },
)
