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
