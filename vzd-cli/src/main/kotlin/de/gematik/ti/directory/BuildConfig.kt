package de.gematik.ti.directory

import java.util.*

object BuildConfig {
    private val properties = Properties()

    init {
        de.gematik.ti.directory.BuildConfig.properties.load(de.gematik.ti.directory.BuildConfig::class.java.getResource("/project.properties").openStream())
    }

    val APP_VERSION: String
        get() = de.gematik.ti.directory.BuildConfig.properties["project.version"] as String
}
