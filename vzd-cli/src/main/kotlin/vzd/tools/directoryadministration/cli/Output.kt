package vzd.tools.directoryadministration.cli

import com.github.doyaaaaaken.kotlincsv.dsl.csvWriter
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encodeToString
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import net.mamoe.yamlkt.Yaml
import vzd.tools.directoryadministration.CertificateDataDER
import vzd.tools.directoryadministration.CertificateInfo
import vzd.tools.directoryadministration.toCertificateInfo
import java.io.ByteArrayOutputStream

/**
 * Special Serializer to display the textual summary of the X509Certificate
 */
object CertificateDataDERInfoSerializer : KSerializer<CertificateDataDER> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("CertificateDataDER", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: CertificateDataDER) {
        val surrogate = value.toCertificateInfo()
        encoder.encodeSerializableValue(CertificateInfo.serializer(), surrogate)
    }

    override fun deserialize(decoder: Decoder): CertificateDataDER {
        throw UnsupportedOperationException()
    }
}

val printerSerializersModule = SerializersModule {
    contextual(CertificateDataDERInfoSerializer)
}

object Output {
    private val yamlOptimized = Yaml { serializersModule = printerSerializersModule }
    private val jsonOptimized = Json {
        prettyPrint = true
        serializersModule = printerSerializersModule
    }
    private val json = Json {
        prettyPrint = true
    }
    private val csv = csvWriter()


    fun printYamlOptimized(value: Any?) {
        println(yamlOptimized.encodeToString(value))
    }

    fun printYaml(value: Any?) {
        println(Yaml.encodeToString(value))
    }

    fun printJsonOptimized(value: Any?) {
        println(jsonOptimized.encodeToString(value))
    }

    fun printJson(value: Any?) {
        println(json.encodeToString(value))
    }

    fun printCsv(value: List<Any?>) {
        val out = ByteArrayOutputStream()
        csv.writeAll(listOf(value), out)
        println(String(out.toByteArray(), Charsets.UTF_8))
    }
}
