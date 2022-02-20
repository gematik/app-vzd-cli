package vzd.admin.cli

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
import vzd.tools.directoryadministration.*
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

/**
 * Human friendly serializer for DN
 */
object DistinguishedNameSerializer: KSerializer<DistinguishedName> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("DistinguishedName", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: DistinguishedName) {
        encoder.encodeString("uid=${value.uid}")
    }

    override fun deserialize(decoder: Decoder): DistinguishedName {
        throw UnsupportedOperationException()
    }
}


val optimizedSerializersModule = SerializersModule {
    contextual(CertificateDataDERInfoSerializer)
    contextual(DistinguishedNameSerializer)
}

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

    inline fun <reified T>printJson(value: T) {
        println(json.encodeToString(value))
    }

    fun printCsv(value: List<Any?>) {
        val out = ByteArrayOutputStream()
        csv.writeAll(listOf(value), out)
        print(String(out.toByteArray(), Charsets.UTF_8))
    }
}

val DirectoryEntryCsvHeaders = listOf(
    "query",
    "telematikID",
    "displayName",
    "streetAddress",
    "postalCode",
    "localityName",
    "stateOrProvinceName",
    "certificateCount",
    "kimAdresses"
)

val DirectoryEntryOutputMapping = mapOf(
    OutputFormat.HUMAN to { _: Map<String, String>, value: List<DirectoryEntry>? -> Output.printHuman(value) },
    OutputFormat.YAML to { _: Map<String, String>, value: List<DirectoryEntry>? -> Output.printYaml(value) },
    OutputFormat.JSON to { _: Map<String, String>, value: List<DirectoryEntry>? -> Output.printJson(value) },
    OutputFormat.SHORT to { _: Map<String, String>, value: List<DirectoryEntry>? ->
        value?.forEach {
            println("${it.directoryEntryBase.dn?.uid} ${it.directoryEntryBase.telematikID} ${Json.encodeToString(it.directoryEntryBase.displayName)}")
        }
    },
    OutputFormat.CSV to { query: Map<String, String>, value: List<DirectoryEntry>? ->

        value?.forEach {
            Output.printCsv(listOf(
                query.toString(),
                it.directoryEntryBase.telematikID.escape(),
                it.directoryEntryBase.displayName,
                it.directoryEntryBase.streetAddress,
                it.directoryEntryBase.postalCode,
                it.directoryEntryBase.localityName,
                it.directoryEntryBase.stateOrProvinceName,
                it.userCertificates?.size.toString(),
                it.fachdaten?.let { it.mapNotNull { it.fad1 }.mapNotNull { it.mapNotNull { it.mail } } }?.flatten()
                    ?.flatten()?.joinToString()
            ))
        }

        if (value == null || value.isEmpty()) {
            Output.printCsv(listOf(query.toString(), "Not Found"))
        }

    },
)