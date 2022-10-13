package de.gematik.ti.directory.cli.admin

import de.gematik.ti.directory.admin.DirectoryEntry
import de.gematik.ti.directory.cli.escape
import hu.vissy.texttable.dsl.tableFormatter
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

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

val DirectoryEntryOutputMapping = mapOf(
    OutputFormat.HUMAN to { _: Map<String, String>, value: List<DirectoryEntry>? -> Output.printHuman(value) },
    OutputFormat.YAML to { _: Map<String, String>, value: List<DirectoryEntry>? -> Output.printYaml(value) },
    OutputFormat.JSON to { _: Map<String, String>, value: List<DirectoryEntry>? -> Output.printJson(value) },
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

            stateless<String>("TelematikID") {
                extractor { directoryEntry ->
                    directoryEntry.directoryEntryBase.telematikID
                }
            }

            stateless<String>("Name") {
                extractor { directoryEntry ->
                    directoryEntry.directoryEntryBase.displayName
                }
                cellFormatter {
                    maxWidth = 24
                }
            }

            stateless<String>("Address") {
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
        }
        println(formatter.apply(value))
    }

)
