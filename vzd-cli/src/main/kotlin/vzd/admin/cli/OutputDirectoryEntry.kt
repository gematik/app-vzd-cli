package vzd.admin.cli

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import vzd.admin.client.DirectoryEntry

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
    "userCertificateCount",
    "userCertificateSerial",
    "userCertificateOCSPStatus",
    "mailCount",
    "mail"
)

val DirectoryEntryOutputMapping = mapOf(
    OutputFormat.HUMAN to { _: Map<String, String>, value: List<DirectoryEntry>? -> Output.printHuman(value) },
    OutputFormat.YAML to { _: Map<String, String>, value: List<DirectoryEntry>? -> Output.printYaml(value) },
    OutputFormat.JSON to { _: Map<String, String>, value: List<DirectoryEntry>? -> Output.printJson(value) },
    OutputFormat.SHORT to { _: Map<String, String>, value: List<DirectoryEntry>? ->
        value?.forEach {
            val kims = it.fachdaten?.let { it.mapNotNull { it.fad1 }.map { it.mapNotNull { it.mail } } }
                ?.flatten()?.flatten()?.joinToString() ?: ""
            println("${it.directoryEntryBase.telematikID} ${Json.encodeToString(it.directoryEntryBase.displayName)}" +
                    " ${it.directoryEntryBase.domainID?.joinToString()}" +
                    " ${kims}")
        }
    },
    OutputFormat.CSV to { query: Map<String, String>, value: List<DirectoryEntry>? ->

        value?.forEach {
            Output.printCsv(listOf(
                query.toString(),
                it.directoryEntryBase.telematikID.escape(),
                it.directoryEntryBase.domainID?.joinToString(),
                it.directoryEntryBase.holder?.joinToString(),
                it.directoryEntryBase.displayName,
                it.directoryEntryBase.streetAddress,
                it.directoryEntryBase.postalCode,
                it.directoryEntryBase.localityName,
                it.directoryEntryBase.stateOrProvinceName,
                it.userCertificates?.count { it.userCertificate != null },
                it.userCertificates?.mapNotNull {it.userCertificate?.certificate }?.map { it.serialNumber.toString() }?.joinToString(),
                it.userCertificates?.mapNotNull {it.userCertificate?.certificateInfo }?.map { it.ocspResponse?.status }?.joinToString(),
                it.fachdaten?.let { it.mapNotNull { it.fad1 }.map { it.mapNotNull { it.mail } } }?.flatten()?.flatten()?.count(),
                it.fachdaten?.let { it.mapNotNull { it.fad1 }.map { it.mapNotNull { it.mail } } }?.flatten()
                    ?.flatten()?.joinToString()
            ))
        }

        if (value == null || value.isEmpty()) {
            Output.printCsv(listOf(query.toString(), "Not Found"))
        }

    },
)
