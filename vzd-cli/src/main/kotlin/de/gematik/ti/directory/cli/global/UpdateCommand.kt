package de.gematik.ti.directory.cli.global

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import de.gematik.ti.directory.cli.BuildConfig
import de.gematik.ti.directory.cli.catching
import de.gematik.ti.directory.global.GlobalAPI
import kotlinx.coroutines.runBlocking
import me.tongfei.progressbar.ProgressBar

class UpdateCommand : CliktCommand(name = "update", help = "Updates this software") {
    private val reinstall by option("-r", "--reinstall", help = "Force re-install").flag()
    private val version by argument(help = "Version to install, latest version is installed by default").default("latest")
    override fun run() = catching {
        val globalAPI = GlobalAPI()

        val updateToVersion = if (reinstall) {
            BuildConfig.APP_VERSION
        } else if (version == "latest") {
            val latestRelease = runBlocking { globalAPI.getLatestVersion() }
            if (latestRelease == BuildConfig.APP_VERSION) {
                echo("No updates available")
                return@catching
            }
            echo("Newer Release is available: $latestRelease (current: ${BuildConfig.APP_VERSION})")
            latestRelease
        } else {
            version
        }

        var progressBar: ProgressBar? = null
        try {
            runBlocking {
                globalAPI.installVersion(updateToVersion) { bytesSentTotal, contentLength ->
                    if (progressBar == null) {
                        progressBar = ProgressBar(updateToVersion, contentLength / 1024 / 1024)
                    }
                    progressBar?.maxHint(contentLength / 1024 / 1024)
                    progressBar?.stepTo(bytesSentTotal / 1024 / 1024)
                }
            }
        } finally {
            progressBar?.maxHint(progressBar?.current ?: 0)
            progressBar?.close()
        }
    }
}
