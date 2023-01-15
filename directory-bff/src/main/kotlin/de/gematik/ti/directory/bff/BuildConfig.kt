package de.gematik.ti.directory.bff

import java.util.*

object BuildConfig {
    private val properties = Properties()
    init {
        properties.load(
            BuildConfig::class.java.classLoader.getResourceAsStream("de.gematik.ti.directory.bff.properties"),
        )
    }

    val APP_VERSION: String
        get() = properties["project.version"] as String
}
