package de.gematik.ti.directory.cli.pers

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.multiple
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.path
import de.gematik.ti.directory.admin.BaseDirectoryEntry
import de.gematik.ti.directory.cli.toJsonPrettyNoDefaults
import de.gematik.ti.directory.cli.toYamlNoDefaults
import de.gematik.ti.directory.pki.CertificateDataDER
import de.gematik.ti.directory.util.escape
import mu.KotlinLogging
import org.bouncycastle.util.encoders.Base64
import org.w3c.dom.Node
import org.w3c.dom.NodeList
import org.w3c.dom.Text
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.xpath.XPathConstants
import javax.xml.xpath.XPathFactory
import kotlin.io.path.writeBytes
import kotlin.io.path.writeText

fun NodeList.forEach(action: (Node) -> Unit) {
    (0 until this.length)
        .asSequence()
        .map { this.item(it) }
        .forEach { action(it) }
}
fun NodeList.asSequence(): Sequence<Node> {
    return (0 until this.length)
        .asSequence()
        .map { this.item(it) }
}

class ExtractCommand : CliktCommand(name = "extract", help = """Extract data from SMC-B/HBA Exports and ObjectSystem files""".trimMargin()) {
    private val logger = KotlinLogging.logger {}
    private val sourceFiles by argument().path(mustBeReadable = true).multiple(required = true)
    private val outputDir by option("-o", "--output-dir", metavar = "OUTPUT_DIR", help = "Output directory for files")
        .path(mustExist = true, canBeFile = false)
        .required()
    // .default(Paths.get(""))

    private val documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
    private val xpath = XPathFactory.newInstance().newXPath()

    override fun run() {
        sourceFiles.forEach { sourceFile ->
            val doc = documentBuilder.parse(sourceFile.toFile())
            // TODO: implement namespaces
            val smcbs = xpath.evaluate("//*[local-name(.)='SmcbAntragExport']", doc, XPathConstants.NODESET) as NodeList

            logger.debug { "Processing ${smcbs.length} entries" }

            smcbs.forEach { antrag ->
                val base = institutionToBaseEntry(antrag)
                writeBaseEntry(base)
                echo("${base.telematikID}: ${base.displayName}")
                certificates(antrag).forEach { cert ->
                    writeCert(cert)
                }
            }

            // gematik card file
            val objects = xpath.evaluate(
                "//child[@id='EF.C.HCI.ENC.R2048' or @id='EF.C.HCI.ENC.E256']/attributes/attribute[@id='body']/text()",
                doc,
                XPathConstants.NODESET,
            ) as NodeList

            var firstCert = true

            objects.forEach { node ->
                val hex = (node as Text).textContent
                val cert = CertificateDataDER(Base64.toBase64String(hex.hexStringToByteArray()))

                if (firstCert) {
                    val base = baseEntryFromCert(cert)
                    writeBaseEntry(base)
                    echo("${base.telematikID}: ${base.displayName}")
                    firstCert = false
                }

                writeCert(cert)
            }
        }
    }

    private fun writeBaseEntry(base: BaseDirectoryEntry) {
        val jsonPath = outputDir.resolve("${base.telematikID.escape()}.json")
        jsonPath.writeText(base.toJsonPrettyNoDefaults())
        val yamlPath = outputDir.resolve("${base.telematikID.escape()}.yaml")
        yamlPath.writeText(base.toYamlNoDefaults())
    }

    private fun baseEntryFromCert(cert: CertificateDataDER): BaseDirectoryEntry {
        val base = BaseDirectoryEntry(cert.certificateInfo.admissionStatement.registrationNumber, entryType = listOf("1"))
        base.displayName = cert.certificateInfo.subjectInfo.cn
        base.givenName = cert.certificateInfo.subjectInfo.givenName
        base.sn = cert.certificateInfo.subjectInfo.sn

        base.countryCode = cert.certificateInfo.subjectInfo.countryCode
        base.stateOrProvinceName = cert.certificateInfo.subjectInfo.stateOrProvinceName
        base.postalCode = cert.certificateInfo.subjectInfo.postalCode
        base.localityName = cert.certificateInfo.subjectInfo.localityName
        base.streetAddress = cert.certificateInfo.subjectInfo.streetAddress
        return base
    }

    private fun writeCert(cert: CertificateDataDER) {
        echo("└── ${cert.certificateInfo.serialNumber}")
        val filenameBase =
            "${cert.certificateInfo.admissionStatement.registrationNumber.escape()}-${cert.certificateInfo.serialNumber}"
        outputDir.resolve("$filenameBase.der").writeBytes(Base64.decode(cert.base64String))
        outputDir.resolve("$filenameBase.certinfo.yaml").writeText(cert.certificateInfo.toYamlNoDefaults())
    }

    private fun certificates(antrag: Node): Sequence<CertificateDataDER> {
        val certs = xpath.evaluate(".//*[local-name(.)='Zertifikate'][starts-with(CertificateSem, 'C.HCI.ENC')]", antrag, XPathConstants.NODESET) as NodeList

        return certs.asSequence().map { certNode ->
            val der = evalString("CertificateValue", certNode)
            CertificateDataDER(der)
        }
    }

    private fun institutionToBaseEntry(antrag: Node): BaseDirectoryEntry {
        val base = BaseDirectoryEntry(xpath.evaluate("Institution/TelematikID", antrag), entryType = listOf("1"))
        base.displayName = evalString("Institution/InstName", antrag)
        base.streetAddress = evalString("Institution/Anschrift/StrassenAdresse/Strasse", antrag) + " " + evalString("Institution/Anschrift/StrassenAdresse/Hausnummer", antrag)
        base.postalCode = evalString("Institution/Anschrift/StrassenAdresse/Postleitzahl", antrag)
        base.localityName = evalString("Institution/Anschrift/StrassenAdresse/Ort", antrag)
        base.countryCode = evalString("Institution/Anschrift/StrassenAdresse/Land", antrag)
        return base
    }

    private fun evalString(xpathExpr: String, node: Node): String {
        return xpath.evaluate(xpathExpr, node).trim()
    }
}

private val HEX_CHARS = "0123456789ABCDEF"

fun String.hexStringToByteArray(): ByteArray {
    val result = ByteArray(length / 2)

    for (i in 0 until length step 2) {
        val firstIndex = HEX_CHARS.indexOf(this[i])
        val secondIndex = HEX_CHARS.indexOf(this[i + 1])

        val octet = firstIndex.shl(4).or(secondIndex)
        result.set(i.shr(1), octet.toByte())
    }

    return result
}
