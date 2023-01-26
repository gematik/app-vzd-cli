package de.gematik.ti.directory.cli.admin

import com.github.doyaaaaaken.kotlincsv.client.CsvWriter
import com.github.doyaaaaaken.kotlincsv.dsl.csvWriter
import de.gematik.ti.directory.admin.DirectoryEntry
import de.gematik.ti.directory.util.escape
import java.io.ByteArrayOutputStream

val DirectoryEntryCsvHeaders = listOf(
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
    "specialization",
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
                        it.directoryEntryBase.specialization?.map { it }?.joinToString() ?: "",
                    ),
                ),
            )
        }
    }
}
