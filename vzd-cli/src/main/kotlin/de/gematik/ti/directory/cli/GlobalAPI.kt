package de.gematik.ti.directory.cli

import de.gematik.ti.directory.DirectoryException
import de.gematik.ti.directory.pki.ListOfTrustedServiceLists
import de.gematik.ti.directory.pki.PKIClient
import de.gematik.ti.directory.pki.loadAllEnvironments
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
import mu.KotlinLogging
import net.mamoe.yamlkt.Yaml
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

const val GITHUB_RELEASES_URL = "https://api.github.com/repos/gematik/app-vzd-cli/releases"

private val JSON =
    Json {
        ignoreUnknownKeys = true
    }

class GlobalAPI {
    val config by lazy { loadConfig() }

    fun loadConfig(): GlobalConfig = GlobalConfigFileStore().value

    fun updateConfig() {
        val store = GlobalConfigFileStore()
        store.value = config
        store.save()
    }

    fun resetConfig(): GlobalConfig {
        val store = GlobalConfigFileStore()
        return store.reset()
    }

    private fun createHttpClient(): HttpClient {
        val httpClient =
            HttpClient(CIO) {
                engine {
                    if (config.httpProxy.enabled) {
                        proxy = ProxyBuilder.http(config.httpProxy.proxyURL)
                    }
                }
            }
        return httpClient
    }

    suspend fun getLatestVersion(): String {
        val httpClient = createHttpClient()

        logger.info { "Checking for updates at: $GITHUB_RELEASES_URL" }

        val jsonString = httpClient.get(GITHUB_RELEASES_URL).body<String>()
        val releases = JSON.decodeFromString<List<Release>>(jsonString)

        val latestRelease =
            if (config.updates.preReleasesEnabled) {
                releases.first()
            } else {
                releases.first { it.preRelease == false }
            }

        config.updates.lastCheck = Instant.now().epochSecond
        config.updates.latestRelease = latestRelease.name
        updateConfig()

        return latestRelease.name
    }

    suspend fun dailyUpdateCheck(): String {
        if ((Instant.now().epochSecond - config.updates.lastCheck) > 24 * 60 * 60) {
            return getLatestVersion()
        }
        return config.updates.latestRelease
    }

    suspend fun installVersion(
        version: String,
        progressListener: ProgressListener?,
    ) {
        if (version < "2.1.0") {
            throw DirectoryException("Updates only supported from version 2.1.0")
        }

        logger.debug { "Trying to determine APP_HOME" }

        var appHome =
            runCatching {
                Path(Cli::class.java.protectionDomain.codeSource.location.file).parent.parent
            }.recover {
                Path(
                    Cli::class.java.protectionDomain.codeSource.location.file
                        .substring(1),
                ).parent.parent
            }.getOrThrow()

        logger.info { "Updating app in $appHome" }

        val updateUrl = "https://github.com/gematik/app-vzd-cli/releases/download/$version/vzd-cli-$version.zip"
        val httpClient = createHttpClient()

        logger.debug { "Downloading update from $updateUrl" }

        val response =
            httpClient.get(updateUrl) {
                onDownload(progressListener)
            }
        if (response.status != HttpStatusCode.OK) {
            throw DirectoryException("Version not found: $version")
        }
        val channel = response.body<ByteReadChannel>()

        // TODO: add GPG signature check
        val zipFile = kotlin.io.path.createTempFile("vzd-cli", ".zip")
        val tempOutput = zipFile.outputStream()
        channel.copyTo(tempOutput)

        val regex = Regex("^vzd-cli-.*?/")
        withContext(Dispatchers.IO) {
            logger.debug { "Processing $zipFile" }
            ZipFile(zipFile.toFile()).use { zip ->
                zip.entries().asSequence().forEach { entry ->
                    if (entry.isDirectory) {
                        return@forEach
                    }
                    if (!entry.name.endsWith(".jar")) {
                        // extract only jars
                        return@forEach
                    }
                    val fileName = entry.name.replace(regex, "")
                    logger.debug { "Extracting $fileName" }

                    val outputFileName = fileName + ".update"
                    val outputFile = Path(appHome.absolutePathString(), *outputFileName.split("/").toTypedArray())

                    zip.getInputStream(entry).use { input ->
                        logger.debug { "Extracting to $outputFileName" }
                        outputFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }

                    try {
                        logger.debug { "Overwriting $fileName" }
                        outputFile.moveTo(Path(appHome.absolutePathString(), *fileName.split("/").toTypedArray()), true)
                    } catch (e: Throwable) {
                        logger.debug { "Unable to overwrite $fileName. Start start script will rename it on next run." }
                    }
                }
            }
        }
        zipFile.deleteExisting()
    }

    val pkiClient by lazy {
        PKIClient {
            if (config.httpProxy.enabled) {
                httpProxyURL = config.httpProxy.proxyURL
            }
            loader = { httpClient ->
                loadCache() ?: run {
                    val cache = loadAllEnvironments(httpClient)
                    saveCache(cache)
                    cache
                }
            }
        }
    }

    private val yaml = Yaml { }

    val cachePath = Path(System.getProperty("user.home"), ".telematik", "tsl-cache.yaml")

    private fun loadCache(): ListOfTrustedServiceLists? =
        if (!cachePath.toFile().exists()) {
            null
        } else {
            val cache: ListOfTrustedServiceLists = yaml.decodeFromString(cachePath.readText())
            if ((Instant.now().epochSecond - cache.lastModified) > 24 * 60 * 60) {
                logger.info { "TSL cache ist older than 24 Hours. Removing it." }
                cachePath.deleteExisting()
                null
            } else {
                cache
            }
        }

    fun saveCache(tslCache: ListOfTrustedServiceLists) {
        cachePath.writeText(yaml.encodeToString(tslCache))
    }
}
