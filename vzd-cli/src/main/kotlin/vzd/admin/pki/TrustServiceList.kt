package vzd.admin.pki

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.decodeFromString
import mu.KotlinLogging
import net.mamoe.yamlkt.Yaml
import org.w3c.dom.NodeList
import org.xml.sax.InputSource
import java.time.Instant
import java.time.format.DateTimeFormatter
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.xpath.XPathConstants
import javax.xml.xpath.XPathFactory
import kotlin.io.path.Path
import kotlin.io.path.deleteExisting
import kotlin.io.path.readText
import kotlin.io.path.writeText

private val logger = KotlinLogging.logger {}

enum class TrustEnvironment {
    TU, RU, PU
}

object TrustServiceListAddresses {
    fun url(env: TrustEnvironment): String {
        return when(env) {
            TrustEnvironment.TU -> "https://download-test.tsl.ti-dienste.de/ECC/ECC-RSA_TSL-test.xml"
            TrustEnvironment.RU -> "https://download-ref.tsl.ti-dienste.de/ECC/ECC-RSA_TSL-ref.xml"
            TrustEnvironment.PU -> "https://download.tsl.ti-dienste.de/ECC/ECC-RSA_TSL.xml"
        }
    }
}

enum class TrustServiceType(val uri: String) {
    CA_NON_QES("http://uri.etsi.org/TrstSvc/Svctype/CA/PKC")
}

@Serializable
data class CAService(
    val env: TrustEnvironment,
    val name: String,
    val caCertificate: CertificateDataDER
    ) {
}

@Serializable
data class TrustedServiceList(
    val caServices: List<CAService>
)

@Serializable
data class TrustedServiceListCache(
    val tu: TrustedServiceList? = null,
    val ru: TrustedServiceList? = null,
    val pu: TrustedServiceList? = null,
    val lastModified: Long = Instant.now().epochSecond,
) {
    val caServices by lazy {
        val result = mutableListOf<CAService>()
        result.addAll(tu?.caServices ?: emptyList())
        result.addAll(ru?.caServices ?: emptyList())
        result.addAll(pu?.caServices ?: emptyList())
        result
    }
    companion object {
        private val YAML = Yaml {  }

        val cachePath = Path(System.getProperty("user.home"), ".telematik", "tsl-cache.yaml")

        fun load(): TrustedServiceListCache?  {

            return if (!cachePath.toFile().exists()) {
                null
            } else {
                val cache: TrustedServiceListCache = YAML.decodeFromString(cachePath.readText())
                if ((Instant.now().epochSecond - cache.lastModified) > 24*60*60) {
                    logger.info { "TSL cache ist older than 24 Hours. Removing it." }
                    cachePath.deleteExisting()
                    null
                } else {
                    cache
                }
            }

        }

        fun save(tslCache: TrustedServiceListCache) {
            cachePath.writeText(YAML.encodeToString(tslCache))
        }

    }
}



private val documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
private val xpath = XPathFactory.newInstance().newXPath()

class TSLLoader(private val httpClient: HttpClient) {
    fun load(env: TrustEnvironment): TrustedServiceList {
        val caServices = mutableListOf<CAService>()
        runBlocking {
            val response = httpClient.get(TrustServiceListAddresses.url(env))
            val body = response.body<String>()
            val document = documentBuilder.parse(InputSource(body.reader()))

            // TODO: implement namespaces
            val serviceNodeList = xpath.evaluate("/TrustServiceStatusList/TrustServiceProviderList/TrustServiceProvider/TSPServices/TSPService[ServiceInformation/ServiceTypeIdentifier='${TrustServiceType.CA_NON_QES.uri}']", document, XPathConstants.NODESET) as NodeList

            var i=0
            while(i<serviceNodeList.length) {
                val serviceNode = serviceNodeList.item(i)
                val certData: String = xpath.evaluate("ServiceInformation/ServiceDigitalIdentity/DigitalId/X509Certificate", serviceNode)
                val name: String = xpath.evaluate("ServiceInformation/ServiceName/Name", serviceNode)
                caServices.add(CAService(env, name, CertificateDataDER(certData)))
                i++
            }

        }
        return TrustedServiceList(caServices)
    }
}

