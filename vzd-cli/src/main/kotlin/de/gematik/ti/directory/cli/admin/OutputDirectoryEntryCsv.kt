package de.gematik.ti.directory.cli.admin

import com.github.doyaaaaaken.kotlincsv.client.CsvWriter
import com.github.doyaaaaaken.kotlincsv.dsl.csvWriter
import de.gematik.ti.directory.admin.DirectoryEntry
import de.gematik.ti.directory.util.escape
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.ByteArrayOutputStream

val DirectoryEntryCsvHeaders = listOf(
    "telematikID",
    "domainID",

    "displayName",
    "otherName",
    "organization",
    "givenName",
    "sn",
    "title",

    "streetAddress",
    "postalCode",
    "localityName",
    "stateOrProvinceName",
    "countryCode",

    "professionOID",
    "specialization",
    "entryType",

    "holder",
    "dataFromAuthority",
    "personalEntry",
    "changeDateTime",

    "maxKOMLEadr",

    "active",
    "meta",

    "userCertificateCount",
    "mailCount",
    "mail",
    "komLeData",
    "FAD",
)

fun listToCsvLine(csvWriter: CsvWriter, value: List<Any?>): String {
    val out = ByteArrayOutputStream()
    csvWriter.writeAll(listOf(value), out)
    return String(out.toByteArray(), Charsets.UTF_8)
}

fun List<DirectoryEntry>.toCsv(): String {
    val csvWriter = csvWriter() {
        delimiter = ';'
    }

    val list = this
    return buildString {
        append('\uFEFF')
        append(listToCsvLine(csvWriter, DirectoryEntryCsvHeaders))
        list.forEach {
            append(
                listToCsvLine(
                    csvWriter,
                    listOf(
                        it.directoryEntryBase.telematikID.escape(),
                        it.directoryEntryBase.domainID?.joinToString("|"),

                        it.directoryEntryBase.displayName,
                        it.directoryEntryBase.otherName,
                        it.directoryEntryBase.organization,
                        it.directoryEntryBase.givenName,
                        it.directoryEntryBase.sn,
                        it.directoryEntryBase.title,

                        it.directoryEntryBase.streetAddress,
                        it.directoryEntryBase.postalCode,
                        it.directoryEntryBase.localityName,
                        it.directoryEntryBase.stateOrProvinceName,
                        it.directoryEntryBase.countryCode,

                        it.directoryEntryBase.professionOID?.joinToString("|"),
                        it.directoryEntryBase.specialization?.joinToString("|"),
                        it.directoryEntryBase.entryType?.joinToString("|"),

                        it.directoryEntryBase.holder?.joinToString("|"),
                        it.directoryEntryBase.dataFromAuthority,
                        it.directoryEntryBase.personalEntry,
                        it.directoryEntryBase.changeDateTime?.toLocalDateTime(TimeZone.of("CET")),

                        it.directoryEntryBase.maxKOMLEadr,

                        it.directoryEntryBase.active,
                        it.directoryEntryBase.meta?.joinToString("|") { Json.encodeToString(this) },

                        it.userCertificates?.count { it.userCertificate != null } ?: 0,
                        it.fachdaten?.let { it.mapNotNull { it.fad1 }.map { it.mapNotNull { it.mail } } }?.flatten()?.flatten()?.count() ?: 0,
                        it.fachdaten?.let { it.mapNotNull { it.fad1 }.map { it.mapNotNull { it.mail } } }?.flatten()
                            ?.flatten()?.joinToString("|"),
                        it.fachdaten?.let { it.mapNotNull { it.fad1 }.map { it.mapNotNull { it.komLeData } } }?.flatten()
                            ?.flatten()?.joinToString("|") { "${it.version},${it.mail}" },
                        it.fachdaten?.let { it.mapNotNull { it.fad1 }.flatten().mapNotNull { it.dn.ou?.firstOrNull() }.joinToString("|") },
                    ),
                ),
            )
        }
    }
}
