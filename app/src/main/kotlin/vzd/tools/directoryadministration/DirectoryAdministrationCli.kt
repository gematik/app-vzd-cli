package vzd.tools.directoryadministration

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.UsageError
import com.github.ajalt.clikt.core.requireObject
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.multiple
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.choice
import io.github.cdimascio.dotenv.Dotenv
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.*
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import mu.KotlinLogging
import net.mamoe.yamlkt.Yaml
import org.bouncycastle.util.encoders.Base64
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate

private val logger = KotlinLogging.logger {}

@Serializable
@SerialName("CertificateDataDER")
data class CertificateDataDERSurrogate (
    val subject: String,
    val issuer: String,
    val signatureAlgorithm: String,
    val publicKeyAlgorithm: String
) {
    companion object Factory {
        fun convert(base64String: String): CertificateDataDERSurrogate {
            val bytes = Base64.decode(base64String)
            val cf = CertificateFactory.getInstance("X.509")
            val cert: X509Certificate = cf.generateCertificate(bytes.inputStream()) as X509Certificate

            cert.publicKey.algorithm

            return CertificateDataDERSurrogate(
                cert.subjectDN.name,
                cert.issuerDN.name,
                cert.sigAlgName,
                cert.publicKey.algorithm
            )
        }

    }
}

object CertificateDataDERPrinterSerializer : KSerializer<CertificateDataDER> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("CertificateDataDER", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: CertificateDataDER) {
        val surrogate = CertificateDataDERSurrogate.Factory.convert(value.base64String)
        encoder.encodeSerializableValue(CertificateDataDERSurrogate.serializer(), surrogate);
    }

    override fun deserialize(decoder: Decoder): CertificateDataDER {
        return CertificateDataDER(decoder.decodeString())
    }
}

val printerSerializersModule = SerializersModule {
    contextual(CertificateDataDERPrinterSerializer)
}


val DirectoryEntryPrinters = mapOf(
    "short" to { value: List<DirectoryEntry>?, _: Boolean, _: Array<String>? ->
        value?.forEach {
            println("${it.directoryEntryBase.dn?.uid} ${it.directoryEntryBase.telematikID} ${Json.encodeToString(it.directoryEntryBase.displayName)}")
        }
    },
    "yaml" to { value: List<DirectoryEntry>?, showCert: Boolean, _: Array<String>? ->
        println(Yaml {
            if (showCert) {
                serializersModule = printerSerializersModule
            }
        }.encodeToString(value))
    },
    "json" to { value: List<DirectoryEntry>?, showCert: Boolean, _: Array<String>? ->
        println(Json {
            prettyPrint = true
            if (showCert) {
                serializersModule = printerSerializersModule
            }
        }.encodeToString(value))
    }
)

class ListDirectoryEntries: CliktCommand(name = "list", help="List directory entries") {
    private val parameters by argument(name="KEY=VALUE").multiple(required = true)
    private val sync by option(help="use Sync mode").flag()
    private val printer by option(help="How the entries should be displayed").choice(*DirectoryEntryPrinters.keys.toTypedArray()).default("short")
    private val showCert by option("--show-cert", help="Show or hide the content of X509 certificate").flag("--hide-cert", default = true)
    private val client by requireObject<Client>();
    override fun run() {
        val paramsMap = parameters.map {
            val kv = it.split("=")
            if (kv.size != 2) {
                throw UsageError("Bad key/value pair: $it")
            }

            Pair(kv[0], kv[1] )
        }.associateBy ( { it.first }, { it.second } )

        val result: List<DirectoryEntry>?
        if (sync) {
            result = runBlocking {  client.readDirectoryEntryForSync( paramsMap ) }
        } else {
            result = runBlocking {  client.readDirectoryEntry( paramsMap ) }
        }

        DirectoryEntryPrinters[printer]?.invoke(result, showCert, null)
    }

}

class AuthenticateAdmin: CliktCommand(name="auth", help="Perform authentication") {
    private val dotenv by requireObject<Dotenv>()
    override fun run() {
        logger.debug { "Executing command: AuthenticateAdmin" }
        val auth = ClientCredentialsAuthenticator(dotenv["ADMIN_AUTH_URL"])
        val tokens = auth.authenticate(dotenv["ADMIN_CLIENT_ID"], dotenv["ADMIN_CLIENT_SECRET"])
        println (tokens.accessToken)
    }
}

class DeleteDiectoryEntry: CliktCommand(name="delete", help="Delete specified directory entries") {
    val uid by argument(help="List of UIDs for to be deleted directory entries").multiple(required = true)
    val force by option(help="Force delete").flag()
    private val client by requireObject<Client>()

    override fun run() {
        if (force) {
            logger.debug { "Deleting {uid}" }
            runBlocking {
                uid.forEach { client.deleteDirectoryEntry( it ) }
            }
        } else {
            throw UsageError("Specify --force option")
        }

    }
}


class DirectoryAdministrationCli : CliktCommand(name="admin", help="""CLI for DirectoryAdministration API

Commands require following environment variables:
 
```
 - ADMIN_AUTH_URL
 - ADMIN_CLIENT_ID
 - ADMIN_CLIENT_SECRET
 - ADMIN_API_URL
 - ADMIN_ACCESS_TOKEN (optional)
``` 
""".trimMargin()) {
    private val dotenv by requireObject<Dotenv>()
    override fun run() {
        currentContext.obj = Client {
            apiURL = dotenv["ADMIN_API_URL"]
            loadTokens = {
                val auth = ClientCredentialsAuthenticator(dotenv["ADMIN_AUTH_URL"])
                auth.authenticate(dotenv["ADMIN_CLIENT_ID"], dotenv["ADMIN_CLIENT_SECRET"])
            }
        }
    }
    init {
        subcommands(AuthenticateAdmin(), ListDirectoryEntries(), DeleteDiectoryEntry())
    }
}