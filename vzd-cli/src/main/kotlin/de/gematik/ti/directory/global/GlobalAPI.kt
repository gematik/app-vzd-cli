package de.gematik.ti.directory.global

import de.gematik.ti.directory.cli.BuildConfig
import de.gematik.ti.directory.cli.Cli
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
import io.ktor.utils.io.jvm.javaio.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import mu.KotlinLogging
import java.time.Instant
import java.util.zip.ZipFile
import kotlin.io.path.*

private val logger = KotlinLogging.logger {}

@Serializable
data class Release(
    val name: String,
    @SerialName("prerelease")
    val preRelease: Boolean,
    @SerialName("published_at")
    val publishedAt: String,
)

const val GITHUB_RELEASES_URL="https://api.github.com/repos/gematik/app-vzd-cli/releases"

private val JSON = Json {
    ignoreUnknownKeys = true
}

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

        logger.info { "Checking for updates at: $GITHUB_RELEASES_URL" }

        val jsonString = httpClient.get(GITHUB_RELEASES_URL).body<String>()
        val releases = JSON.decodeFromString<List<Release>>(jsonString)

        val latestRelease = if (config.updates.preReleasesEnabled) {
            releases.first()
        } else  {
            releases.first { it.preRelease == false }
        }

        config.updates.lastCheck = Instant.now().epochSecond
        config.updates.latestRelease = latestRelease.name
        updateConfig()

        return latestRelease.name
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

    suspend fun installVersion(version: String, progressListener: ProgressListener?) {
        if (version < "2.1.0") {
            throw DirectoryException("Updates only supported from version 2.1.0")
        }
        val appHome = Path(Cli::class.java.protectionDomain.codeSource.location.file).parent.parent
        logger.info { "Updating app in $appHome" }

        val updateUrl = "https://github.com/gematik/app-vzd-cli/releases/download/$version/vzd-cli-$version.zip"
        val httpClient = createHttpClient()

        val zipFile = kotlin.io.path.createTempFile("vzd-cli", ".zip") // Path(appHome.absolutePathString(), "vzd-cli-$version.zip")
        logger.debug { "Downloading update from $updateUrl to $zipFile" }

        val response = httpClient.get(updateUrl) {
            onDownload(progressListener)
        }
        if (response.status != HttpStatusCode.OK) {
            throw DirectoryException("Version not found: $version")
        }
        val channel = response.body<ByteReadChannel>()

        // TODO: add GPG signature check
        val tempOutput = zipFile.outputStream()
        channel.copyTo(tempOutput)

        val regex = Regex(".*vzd-cli.*all.jar$")
        val jarFile = Path(appHome.absolutePathString(), "lib", "vzd-cli-all.jar.update")
        withContext(Dispatchers.IO) {
            logger.debug { "Looking for updated jars" }
            ZipFile(zipFile.toFile()).use { zip ->
                zip.entries().asSequence().forEach { entry ->
                    logger.debug { entry.name }
                    if (entry.name.matches(regex)) {
                        logger.debug { "Extracting $entry" }

                        zip.getInputStream(entry).use { input ->
                            jarFile.outputStream().use { output ->
                                input.copyTo(output)
                            }
                        }
                    }
                }
            }
        }

        jarFile.moveTo(Path(appHome.absolutePathString(), "lib", "vzd-cli-all.jar"), true)

        zipFile.deleteExisting()
    }

    val pkiClient by lazy {
        PKIClient {
            if (config.httpProxy.enabled) {
                httpProxyURL = config.httpProxy.proxyURL
            }
        }
    }
}
