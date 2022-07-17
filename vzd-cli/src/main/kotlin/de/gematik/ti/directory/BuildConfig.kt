package de.gematik.ti.directory

import java.util.*

object BuildConfig {
    private val properties = Properties()

    init {
        properties.load(BuildConfig::class.java.getResource("/vzd-cli.properties").openStream())
    }

    val APP_VERSION: String
        get() = properties["project.version"] as String
}
