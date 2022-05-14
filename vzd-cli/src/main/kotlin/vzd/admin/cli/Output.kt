package vzd.admin.cli

import com.github.doyaaaaaken.kotlincsv.dsl.csvWriter
import kotlinx.serialization.KSerializer
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encodeToString
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.decodeStructure
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import net.mamoe.yamlkt.Yaml
import vzd.admin.client.DistinguishedName
import vzd.admin.pki.CertificateDataDER
import vzd.admin.pki.CertificateDataDERInfoSerializer
import vzd.admin.pki.CertificateInfo
import java.io.ByteArrayOutputStream

val optimizedSerializersModule = SerializersModule {
    contextual(CertificateDataDERInfoSerializer)
}

val JsonPretty = Json { prettyPrint = true }


/**
 * Output helper class für human, json, yaml and csv outputs
 */
object Output {
    private val yamlOptimized = Yaml {
        serializersModule = optimizedSerializersModule
        encodeDefaultValues = false
    }
    private val yaml = Yaml {
        encodeDefaultValues = false
    }
    val json = Json {
        prettyPrint = true
    }
    private val csv = csvWriter {
        delimiter = ';'
    }

    fun printHuman(value: Any?) {
        value?.let {
            println(yamlOptimized.encodeToString(value))
        }
    }

    fun printYaml(value: Any?) {
        println(yaml.encodeToString(value))
    }

    inline fun <reified T> printJson(value: T) {
        println(json.encodeToString(value))
    }

    fun printCsv(value: List<Any?>) {
        val out = ByteArrayOutputStream()
        csv.writeAll(listOf(value), out)
        print(String(out.toByteArray(), Charsets.UTF_8))
    }
}

