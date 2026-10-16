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

package de.gematik.ti.directory.pki

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import mu.KotlinLogging
import org.w3c.dom.NodeList
import org.xml.sax.InputSource
import java.time.Instant
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.xpath.XPathConstants
import javax.xml.xpath.XPathFactory

private val logger = KotlinLogging.logger {}

enum class TrustEnvironment {
    TU,
    RU,
    PU,
}

object TrustServiceListAddresses {
    fun url(env: TrustEnvironment): String =
        when (env) {
            TrustEnvironment.TU -> "https://download-test.tsl.ti-dienste.de/ECC/ECC-RSA_TSL-test.xml"
            TrustEnvironment.RU -> "https://download-ref.tsl.ti-dienste.de/ECC/ECC-RSA_TSL-ref.xml"
            TrustEnvironment.PU -> "https://download.tsl.ti-dienste.de/ECC/ECC-RSA_TSL.xml"
        }
}

enum class TrustServiceType(
    val uri: String
) {
    CA_NON_QES("http://uri.etsi.org/TrstSvc/Svctype/CA/PKC"),
}

@Serializable
data class CAService(
    val env: TrustEnvironment,
    val name: String,
    val caCertificate: CertificateDataDER,
)

@Serializable
data class TrustedServiceList(
    val caServices: List<CAService>,
)

@Serializable
data class ListOfTrustedServiceLists(
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
}

private val documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
private val xpath = XPathFactory.newInstance().newXPath()

fun ListOfTrustedServiceLists.Companion.loadFromServer(
    httpClient: HttpClient,
    env: TrustEnvironment,
): TrustedServiceList {
    val caServices = mutableListOf<CAService>()
    runBlocking {
        val response = httpClient.get(TrustServiceListAddresses.url(env))
        val body = response.body<String>()
        val document = documentBuilder.parse(InputSource(body.reader()))

        // TODO: implement namespaces
        val serviceNodeList =
            xpath.evaluate(
                "/TrustServiceStatusList/TrustServiceProviderList/TrustServiceProvider/TSPServices/TSPService[ServiceInformation/ServiceTypeIdentifier='${TrustServiceType.CA_NON_QES.uri}']",
                document,
                XPathConstants.NODESET,
            ) as NodeList

        var i = 0
        while (i < serviceNodeList.length) {
            val serviceNode = serviceNodeList.item(i)
            val certData: String = xpath.evaluate("ServiceInformation/ServiceDigitalIdentity/DigitalId/X509Certificate", serviceNode)
            val name: String = xpath.evaluate("ServiceInformation/ServiceName/Name", serviceNode)
            logger.debug { "Add $env CA '$name'" }
            caServices.add(CAService(env, name, CertificateDataDER(certData)))
            i++
        }
    }
    return TrustedServiceList(caServices)
}
