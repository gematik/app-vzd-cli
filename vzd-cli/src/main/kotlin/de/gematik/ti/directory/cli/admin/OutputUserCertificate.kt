/*
 * Copyright (Change Date see Readme), gematik GmbH
 *
 * Licensed under the EUPL, Version 1.2 or - as soon they will be approved by the
 * European Commission – subsequent versions of the EUPL (the "Licence").
 * You may not use this work except in compliance with the Licence.
 *
 * You find a copy of the Licence in the "Licence" file or at
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either expressed or implied.
 * In case of changes by gematik find details in the "Readme" file.
 *
 * See the Licence for the specific language governing permissions and limitations under the Licence.
 *
 * *******
 *
 * For additional notes and disclaimer from gematik and in case of changes by gematik,
 * find details in the "Readme" file.
 */

package de.gematik.ti.directory.cli.admin

import com.github.ajalt.clikt.core.UsageError
import com.github.doyaaaaaken.kotlincsv.dsl.csvWriter
import de.gematik.ti.directory.admin.UserCertificate
import de.gematik.ti.directory.cli.toJsonPretty
import de.gematik.ti.directory.cli.toYamlNoDefaults
import de.gematik.ti.directory.pki.CertificateDataDER
import de.gematik.ti.directory.pki.ExtendedCertificateDataDERSerializer
import hu.vissy.texttable.dsl.tableFormatter
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import net.mamoe.yamlkt.Yaml

val UserCertificateCsvHeaders =
    listOf(
        "uid",
        "telematikID",
        "entryType",
        "publicKeyAlgorithm",
        "subject",
        "notBefore",
        "notAfter",
        "ocspResponse",
    )

private var humanUserCertificateYaml =
    Yaml {
        encodeDefaultValues = false
        serializersModule =
            SerializersModule {
                contextual(ExtendedCertificateDataDERSerializer)
            }
    }

fun UserCertificate.toHuman() = humanUserCertificateYaml.encodeToString(this)

fun List<UserCertificate>.toHuman() = humanUserCertificateYaml.encodeToString(this)

fun List<UserCertificate>.toCsv(): String {
    val csvWriter =
        csvWriter {
            delimiter = ';'
        }
    val list = this
    return buildString {
        append('\uFEFF')
        append(listToCsvLine(csvWriter, UserCertificateCsvHeaders))
        list.forEach {
            val cert = it.userCertificate?.certificateInfo
            append(
                listToCsvLine(
                    csvWriter,
                    listOf(
                        toString(),
                        it.dn?.uid,
                        it.telematikID,
                        it.entryType,
                        cert?.publicKeyAlgorithm,
                        cert?.subject,
                        cert?.notBefore,
                        cert?.notAfter,
                        cert?.ocspResponse?.status,
                    ),
                ),
            )
        }
    }
}

fun List<UserCertificate>.toTable(): String {
    val formatter =
        tableFormatter<CertificateDataDER> {
            labeled("Serial", "Gesamt") {
                extractor { cert ->
                    cert.certificateInfo.serialNumber
                }
                cellFormatter {
                    maxWidth = 24
                }
            }

            class State(
                var count: Int = 0
            )
            stateful<String, State>("Alg") {
                initState { State() }
                extractor { cert, state ->
                    state.count += 1
                    cert.certificateInfo.publicKeyAlgorithm
                }
                cellFormatter {
                    maxWidth = 3
                }
                aggregator { _, state ->
                    if (state.count > 99) {
                        "99+"
                    } else {
                        state.count.toString()
                    }
                }
            }

            labeled("Subject") {
                extractor { cert ->
                    cert.certificateInfo.subjectInfo.cn
                }
                cellFormatter {
                    maxWidth = 30
                }
            }

            labeled("OCSP") {
                extractor { cert ->
                    cert.certificateInfo.ocspResponse?.status
                }
            }

            labeled("Not After") {
                extractor { cert ->
                    runCatching {
                        cert.certificateInfo.notAfter
                    }.getOrNull()
                }
            }

            showAggregation = true
        }

    return formatter.apply(mapNotNull { it.userCertificate })
}

fun UserCertificate.toStringRepresentation(
    format: RepresentationFormat,
    limitTo: List<RepresentationFormat>? = null,
): String {
    limitTo?.let {
        if (!limitTo.contains(format)) throw UsageError("Cant use '$format' in this command")
    }
    return when (format) {
        RepresentationFormat.HUMAN -> this.toHuman()
        RepresentationFormat.YAML -> this.toYamlNoDefaults()
        RepresentationFormat.JSON -> this.toJsonPretty()
        else -> ""
    }
}

fun List<UserCertificate>.toStringRepresentation(format: RepresentationFormat): String =
    when (format) {
        RepresentationFormat.HUMAN -> this.toHuman()
        RepresentationFormat.YAML -> this.toYamlNoDefaults()
        RepresentationFormat.JSON -> this.toJsonPretty()
        RepresentationFormat.TABLE -> this.toTable()
        RepresentationFormat.CSV -> this.toCsv()
        else -> ""
    }
