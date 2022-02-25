package vzd.admin.cli

import vzd.admin.client.UserCertificate
import vzd.admin.client.toCertificateInfo

val UserCertificateCsvHeaders = listOf(
    "query",
    "uid",
    "telematikID",
    "entryType",
    "publicKeyAlgorithm",
    "subject",
    "notBefore",
    "notAfter",
)

val CertificateOutputMapping = mapOf(
    OutputFormat.HUMAN to { _: Map<String, String>, value: List<UserCertificate>? -> Output.printHuman(value) },
    OutputFormat.YAML to { _: Map<String, String>, value: List<UserCertificate>? -> Output.printYaml(value) },
    OutputFormat.JSON to { _: Map<String, String>, value: List<UserCertificate>?-> Output.printJson(value) },
    OutputFormat.SHORT to { _: Map<String, String>, value: List<UserCertificate>? ->
        value?.forEach {
            val cert = it.userCertificate?.toCertificateInfo()
            println("${it.dn?.uid} ${it.telematikID} ${it.entryType} ${cert?.publicKeyAlgorithm} ${cert?.subject}")
        }
    },
    OutputFormat.CSV to { query: Map<String, String>, value: List<UserCertificate>? ->

        value?.forEach {
            val cert = it.userCertificate?.toCertificateInfo()
            Output.printCsv(listOf(
                query.toString(),
                it.dn?.uid,
                it.telematikID,
                it.entryType,
                cert?.publicKeyAlgorithm,
                cert?.subject,
                cert?.notBefore,
                cert?.notAfter,

                ))
        }

        if (value == null || value.isEmpty()) {
            Output.printCsv(listOf(query.toString(), "Not Found"))
        }

    },
)