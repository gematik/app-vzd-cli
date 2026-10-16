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

package de.gematik.ti.directory.admin

import de.gematik.ti.directory.DirectoryEnvironment
import de.gematik.ti.directory.DirectoryException
import kotlinx.serialization.Serializable

val DefaultConfig =
    Config(
        environments =
            mapOf(
                "tu" to
                    EnvironmentConfig(
                        authURL = "https://auth-test.vzd.ti-dienste.de:9443/auth/realms/RSDirectoryAdministration/protocol/openid-connect/token",
                        apiURL = "https://vzdpflege-test.vzd.ti-dienste.de:9543",
                    ),
                "ru" to
                    EnvironmentConfig(
                        authURL = "https://auth-ref.vzd.ti-dienste.de:9443/auth/realms/RSDirectoryAdministration/protocol/openid-connect/token",
                        apiURL = "https://vzdpflege-ref.vzd.ti-dienste.de:9543/",
                    ),
                "pu" to
                    EnvironmentConfig(
                        authURL = "https://auth.vzd.ti-dienste.de:9443/auth/realms/RSDirectoryAdministration/protocol/openid-connect/token",
                        apiURL = "https://vzdpflege.vzd.ti-dienste.de:9543",
                    ),
            ),
    )

@Serializable
data class Config(
    val environments: Map<String, EnvironmentConfig>,
) {
    fun environment(env: DirectoryEnvironment) = environments[env.name] ?: throw DirectoryException("Unknown environment: ${env.name}")
}

@Serializable
data class EnvironmentConfig(
    val authURL: String,
    val apiURL: String,
)
