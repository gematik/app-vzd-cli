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

import de.gematik.ti.directory.cli.util.FileObjectStore
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import java.nio.file.Path

@Serializable
data class HttpProxyConfig(
    var proxyURL: String,
    var enabled: Boolean,
)

@Serializable
data class UpdatesConfig(
    var preReleasesEnabled: Boolean,
    var lastCheck: Long,
    var latestRelease: String,
    var enabled: Boolean = true,
)

@Serializable
data class GlobalConfig(
    var httpProxy: HttpProxyConfig,
    var updates: UpdatesConfig,
)

internal class GlobalConfigFileStore(
    customConfigPath: Path? = null
) : FileObjectStore<GlobalConfig>(
        "directory-global.yaml",
        {
            GlobalConfig(
                httpProxy =
                    HttpProxyConfig(
                        proxyURL = "http://192.168.110.10:3128/",
                        enabled = false,
                    ),
                updates =
                    UpdatesConfig(
                        preReleasesEnabled = false,
                        lastCheck = -1,
                        latestRelease = BuildConfig.APP_VERSION,
                        enabled = true,
                    ),
            )
        },
        { yaml, stringValue -> yaml.decodeFromString(stringValue) },
        customConfigPath,
    )
