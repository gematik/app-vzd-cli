package de.gematik.ti.directory.global

import de.gematik.ti.directory.cli.BuildConfig
import de.gematik.ti.directory.cli.Cli
import de.gematik.ti.directory.util.PKIClient
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.content.*
import io.ktor.client.engine.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import mu.KotlinLogging
import java.time.Instant
import java.util.zip.ZipFile
import kotlin.io.path.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.name
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

    fun selfUpdate(version: String, progressListener: ProgressListener?) {
        /*
        println(
            System.getenv().map {
                "${it.key} = ${it.value}"
            }
                .joinToString("\n")
        )
         */
        val appHome = Path(Cli::class.java.protectionDomain.codeSource.location.file).parent.parent
        logger.info { "Updating app in $appHome" }

        val updateUrl = "https://github.com/gematik/app-vzd-cli/releases/download/$version/vzd-cli-$version.zip"
        val httpClient = createHttpClient()

        logger.info { "Downloading update from $updateUrl" }
/*
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
 */
        // TODO: add GPG signature check

        val tempFile = Path("/var/folders/fn/5tmqsv5n67q2lnt74spjwdqc0000gn/T/vzd-cli9670087977622399596.zip")

        logger.debug { tempFile }

        ZipFile(tempFile.toFile()).use { zip ->
            zip.entries().asSequence().forEach { entry ->
                if (entry.name.endsWith(".jar")) {
                    var jarFile = Path(appHome.absolutePathString(), "lib", Path(entry.name).fileName.name)

                    logger.debug { "Extracting $jarFile" }
                    zip.getInputStream(entry).use { input ->
                        jarFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                }
            }
        }
    }

    val pkiClient by lazy {
        PKIClient {
            if (config.httpProxy.enabled) {
                httpProxyURL = config.httpProxy.proxyURL
            }
        }
    }
}
