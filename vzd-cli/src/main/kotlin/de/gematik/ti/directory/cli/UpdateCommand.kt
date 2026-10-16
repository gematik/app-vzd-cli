/*
 * Copyright (Change Date see Readme), gematik GmbH
 *
 * Licensed under the EUPL, Version 1.2 or - as soon they will be approved by the
 * European Commission – subsequent versions of the EUPL (the "Licence").
 * You may not use this work except in compliance with the Licence.
 *
 * You find a copy of the Licence in the "Licence" file or at
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either expressed or implied.
 * In case of changes by gematik find details in the "Readme" file.
 *
 * See the Licence for the specific language governing permissions and limitations under the Licence.
 *
 * *******
 *
 * For additional notes and disclaimer from gematik and in case of changes by gematik,
 * find details in the "Readme" file.
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
