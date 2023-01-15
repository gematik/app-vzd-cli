package de.gematik.spegg

import kotlin.io.path.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.reflect.KProperty

fun String.toSnakeCase() = replace(humps, "_").uppercase()
private val humps = "(?<=.)(?=\\p{Upper})".toRegex()

var secretsDir = Path(System.getenv("SECRETS_FOLDER") ?: "/etc/secrets")

class Setting(private val defaultValue: String? = null) {

    operator fun getValue(thisRef: Any?, property: KProperty<*>): String {
        val name = property.name.toSnakeCase()
        return findValue(name, defaultValue)
    }

    fun int(defaultValue: Int? = null): IntSetting {
        return IntSetting(defaultValue)
    }

    fun findValue(name: String, default: String? = null): String {
        val fromEnv = System.getenv()[name]
        if (fromEnv != null) {
            return fromEnv
        }

        val fromProperties = System.getProperties()[name]?.toString()
        if (fromProperties != null) {
            return fromProperties
        }

        val secretFile = Path(secretsDir.absolutePathString(), name)
        if (secretFile.exists()) {
            return secretFile.readText(Charsets.UTF_8)
        }

        if (default != null) {
            return default
        }

        throw Error("$name is not configured, set env variable or provide file in ${secretsDir.absolutePathString()}.")
    }

    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: String) {
    }
}

class IntSetting(val defaultValue: Int? = null) {
    private val setting: Setting
    init {
        setting = Setting(defaultValue = defaultValue?.toString())
    }
    operator fun getValue(thisRef: Any?, property: KProperty<*>): Int {
        return setting.getValue(thisRef, property).toInt()
    }
}

fun checkMandatorySettings(vararg settings: String) {
    val unset = settings.mapNotNull {
        val value = Setting().findValue(it, "<UNSET>")
        if (value == "<UNSET>") {
            it
        } else {
            null
        }
    }.joinToString()

    if (unset.isNotEmpty()) {
        throw Error("Some of the mandatory settings are not set: $unset")
    }
}
