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
