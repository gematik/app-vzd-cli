package de.gematik.ti.directory.cli.admin

import de.gematik.ti.directory.admin.UserCertificate
import de.gematik.ti.directory.util.CertificateDataDER
import hu.vissy.texttable.dsl.tableFormatter
import java.time.LocalDateTime

val UserCertificateCsvHeaders = listOf(
    "query",
    "uid",
    "telematikID",
    "entryType",
    "publicKeyAlgorithm",
    "subject",
    "notBefore",
    "notAfter",
    "ocspResponse"
)

val CertificateOutputMapping = mapOf(
    OutputFormat.HUMAN to { _: Map<String, String>, value: List<UserCertificate>? -> Output.printHuman(value) },
    OutputFormat.YAML to { _: Map<String, String>, value: List<UserCertificate>? -> Output.printYaml(value) },
    OutputFormat.JSON to { _: Map<String, String>, value: List<UserCertificate>? -> Output.printJson(value) },
    OutputFormat.SHORT to { _: Map<String, String>, value: List<UserCertificate>? ->
        value?.forEach {
            val cert = it.userCertificate?.certificateInfo
            println("${it.dn?.uid} ${it.telematikID} ${it.entryType} ${cert?.publicKeyAlgorithm} ${cert?.subject}")
        }
    },
    OutputFormat.CSV to { query: Map<String, String>, value: List<UserCertificate>? ->

        value?.forEach {
            val cert = it.userCertificate?.certificateInfo
            Output.printCsv(
                listOf(
                    query.toString(),
                    it.dn?.uid,
                    it.telematikID,
                    it.entryType,
                    cert?.publicKeyAlgorithm,
                    cert?.subject,
                    cert?.notBefore,
                    cert?.notAfter,
                    cert?.ocspResponse?.status
                )
            )
        }

        if (value.isNullOrEmpty()) {
            Output.printCsv(listOf(query.toString(), "Not Found"))
        }
    },
    OutputFormat.TABLE to { _: Map<String, String>, value: List<UserCertificate>? ->
        val formatter = tableFormatter<CertificateDataDER> {

            labeled("Serial", "Gesamt") {
                extractor { cert ->
                    cert.certificateInfo.serialNumber
                }
                cellFormatter {
                    maxWidth = 24
                }

            }

            class State(var count: Int = 0)
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
                        LocalDateTime.parse(cert.certificateInfo.notAfter).toLocalDate()
                    }.getOrNull()

                }
            }

            showAggregation = true
        }

        println(formatter.apply(value?.mapNotNull { it.userCertificate } ?: emptyList<CertificateDataDER>()))
    },
)
