package de.gematik.ti.directory.global

import de.gematik.ti.directory.cli.BuildConfig
import de.gematik.ti.directory.util.DirectoryException
import de.gematik.ti.directory.util.PKIClient
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.content.*
import io.ktor.client.engine.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.utils.io.*
import io.ktor.utils.io.copyTo
import io.ktor.utils.io.jvm.javaio.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import mu.KotlinLogging
import java.time.Instant
import kotlin.io.path.outputStream

private val logger = KotlinLogging.logger {}

@Serializable
data class Latest(val release: String, val preRelease: String)

class GlobalAPI {
    val config by lazy { loadConfig() }
    fun loadConfig(): GlobalConfig {
        return GlobalConfigFileStore().value
    }
    fun updateConfig() {
        val store = GlobalConfigFileStore()
        store.value = config
        store.save()
    }
    fun resetConfig(): GlobalConfig {
        val store = GlobalConfigFileStore()
        return store.reset()
    }

    suspend fun checkForUpdates(): String {
        val httpClient = createHttpClient()

        logger.info { "Checking for updates at: ${config.updates.checkURL}" }

        val jsonString = httpClient.get(config.updates.checkURL).body<String>()
        val latest = Json.decodeFromString<Latest>(jsonString)
        val latestRelease = if (!config.updates.preReleasesEnabled && latest.release != BuildConfig.APP_VERSION) {
            latest.release
        } else if (config.updates.preReleasesEnabled && latest.preRelease != BuildConfig.APP_VERSION) {
            latest.preRelease
        } else {
            BuildConfig.APP_VERSION
        }

        config.updates.lastCheck = Instant.now().epochSecond
        config.updates.latestRelease = latestRelease
        updateConfig()

        return latestRelease
    }

    private fun createHttpClient(): HttpClient {
        val httpClient = HttpClient(CIO) {
            engine {
                if (config.httpProxy.enabled) {
                    proxy = ProxyBuilder.http(config.httpProxy.proxyURL)
                }
            }
        }
        return httpClient
    }

    suspend fun selfUpdate(version2: String, progressListener: ProgressListener?) {
        val version = "2.0.0"
        val updateUrl = "https://github.com/gematik/app-vzd-cli/releases/download/$version/vzd-cli-$version.zip"
        val httpClient = createHttpClient()

        logger.info { "Downloading update from $updateUrl" }

        val response = httpClient.get(updateUrl) {
            onDownload(progressListener)
        }
        if (response.status != HttpStatusCode.OK) {
            throw DirectoryException("Version not found: $version")
        }
        val channel = response.body<ByteReadChannel>()

        val tempFile = kotlin.io.path.createTempFile("vzd-cli", ".zip")
        val tempOutput = tempFile.outputStream()
        channel.copyTo(tempOutput)
        logger.debug { tempFile }
    }

    val pkiClient by lazy {
        PKIClient {
            if (config.httpProxy.enabled) {
                httpProxyURL = config.httpProxy.proxyURL
            }
        }
    }
}
