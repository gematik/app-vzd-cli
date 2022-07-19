package de.gematik.ti.directory.pers

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.multiple
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.path
import de.gematik.ti.directory.JsonPretty
import de.gematik.ti.directory.YamlPretty
import de.gematik.ti.directory.admin.client.BaseDirectoryEntry
import de.gematik.ti.directory.escape
import de.gematik.ti.directory.pki.CertificateDataDER
import kotlinx.serialization.encodeToString
import mu.KotlinLogging
import org.bouncycastle.util.encoders.Base64
import org.w3c.dom.Node
import org.w3c.dom.NodeList
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

class ExtractCommand : CliktCommand(name = "extract", help = """Extract data from SMC-B/HBA Exports""".trimMargin()) {
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
                val base = instituruinToBaseEntry(antrag)
                val jsonPath = outputDir.resolve("${base.telematikID.escape()}.json")
                jsonPath.writeText(JsonPretty.encodeToString(base))
                val yamlPath = outputDir.resolve("${base.telematikID.escape()}.yaml")
                yamlPath.writeText(YamlPretty.encodeToString(base))
                echo("${base.telematikID}: ${base.displayName}")
                certoficates(antrag).forEach { cert ->
                    echo("└── ${cert.certificateInfo.serialNumber}")
                    val filenameBase = "${cert.certificateInfo.admissionStatement.registrationNumber.escape()}-${cert.certificateInfo.serialNumber}"
                    outputDir.resolve("$filenameBase.der").writeBytes(Base64.decode(cert.base64String))
                    outputDir.resolve("$filenameBase.certinfo.yaml").writeText(YamlPretty.encodeToString(cert.certificateInfo))
                }
            }
        }
    }

    private fun certoficates(antrag: Node): Sequence<CertificateDataDER> {
        val certs = xpath.evaluate(".//*[local-name(.)='Zertifikate'][starts-with(CertificateSem, 'C.HCI.ENC')]", antrag, XPathConstants.NODESET) as NodeList

        return certs.asSequence().map { certNode ->
            val der = evalString("CertificateValue", certNode)
            CertificateDataDER(der)
        }
    }

    private fun instituruinToBaseEntry(antrag: Node): BaseDirectoryEntry {
        val base = BaseDirectoryEntry(xpath.evaluate("Institution/TelematikID", antrag))
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
