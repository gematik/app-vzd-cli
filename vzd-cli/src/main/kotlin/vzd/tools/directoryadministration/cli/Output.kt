package vzd.tools.directoryadministration.cli

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import net.mamoe.yamlkt.Yaml

inline fun <reified E>printYaml(value: E?, showRawCert: Boolean) {
    println(Yaml {
        if (!showRawCert) {
            serializersModule = printerSerializersModule
        }
    }.encodeToString(value))
}

inline fun <reified E>printJson(value: E?, showRawCert: Boolean) {
    println(Json {
        prettyPrint = true
        if (!showRawCert) {
            serializersModule = printerSerializersModule
        }
    }.encodeToString(value))
}
