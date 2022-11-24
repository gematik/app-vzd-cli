package de.gematik.ti.directory.cli.admin

import com.github.doyaaaaaken.kotlincsv.dsl.csvWriter
import de.gematik.ti.directory.util.ExtendedCertificateDataDERSerializer
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import net.mamoe.yamlkt.Yaml
import java.io.ByteArrayOutputStream

@Deprecated("Global module does not make a lot of sense")
private val optimizedSerializersModule = SerializersModule {
    contextual(ExtendedCertificateDataDERSerializer)
}
enum class OutputFormat {
    HUMAN, JSON, YAML, CSV, SHORT, TABLE, JSON_EXT, YAML_EXT
}

/**
 * Output helper class für human, json, yaml and csv outputs
 */
object Output {
    private val yaml = Yaml {
        encodeDefaultValues = false
    }
    val json = Json {
        prettyPrint = true
    }
    private val csv = csvWriter {
        delimiter = ';'
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
