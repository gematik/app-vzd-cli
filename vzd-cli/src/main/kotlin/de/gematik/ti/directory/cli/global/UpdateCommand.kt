package de.gematik.ti.directory.cli.global

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.default
import com.github.ajalt.clikt.parameters.arguments.optional
import de.gematik.ti.directory.cli.BuildConfig
import de.gematik.ti.directory.cli.catching
import de.gematik.ti.directory.global.GlobalAPI
import kotlinx.coroutines.runBlocking
import me.tongfei.progressbar.ProgressBar

class UpdateCommand : CliktCommand(name = "update", help = "Updates this software") {
    private val version by argument(help = "Version to install, latest version is installed by default").default("latest")
    override fun run() = catching {
        val globalAPI = GlobalAPI()

        val updateToVersion = if (version == "latest") {
            val latestRelease = runBlocking { globalAPI.checkForUpdates() }
            if (latestRelease == BuildConfig.APP_VERSION) {
                echo("No updates available")
                return@catching
            }
            echo("Newer Release is available: $version (current: ${BuildConfig.APP_VERSION})")
            latestRelease
        } else {
            version
        }

        val progressBar = ProgressBar("Downloading $updateToVersion", -1)
        try {
            runBlocking {
                globalAPI.selfUpdate(updateToVersion) { bytesSentTotal, contentLength ->
                    progressBar.maxHint(contentLength/1000)
                    progressBar.stepTo(bytesSentTotal/1000)
                }
            }
        } finally {
            progressBar.maxHint(progressBar.current)
            progressBar.close()
        }
    }
}