package vzd

import java.util.*

object BuildConfig {
    private val properties = Properties()

    init {
        properties.load(BuildConfig::class.java.getResource("/project.properties").openStream())
    }

    val APP_VERSION: String
        get() = properties["project.version"] as String
}
