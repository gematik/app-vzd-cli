/*
Copyright 2026, gematik GmbH

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

*******

For additional notes and disclaimer from gematik and in case of changes
by gematik, find details in the "Readme" file.
*/

package de.gematik.ti.directory.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import kotlinx.coroutines.runBlocking
import me.tongfei.progressbar.ProgressBar

class UpdateCommand : CliktCommand(name = "update", help = "Updates this software") {
    private val reinstall by option("-r", "--reinstall", help = "Force re-install").flag()
    private val version by argument(help = "Version to install, latest version is installed by default").default("latest")

    override fun run() =
        catching {
            val globalAPI = GlobalAPI()

            val updateToVersion =
                if (reinstall) {
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
