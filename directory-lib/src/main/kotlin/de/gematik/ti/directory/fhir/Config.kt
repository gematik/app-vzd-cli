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

package de.gematik.ti.directory.fhir

import de.gematik.ti.directory.DirectoryEnvironment
import de.gematik.ti.directory.DirectoryException
import kotlinx.serialization.Serializable

val DefaultConfig =
    Config(
        environments =
            mapOf(
                "tu" to
                    EnvironmentConfig(
                        search =
                            SearchConfig(
                                apiURL = "https://fhir-directory-tu.vzd.ti-dienste.de/search",
                            ),
                        fdv =
                            FdvConfig(
                                apiURL = "https://fhir-directory-tu.vzd.ti-dienste.de/fdv/search",
                                authenticationEndpoint = "https://auth-test.vzd.ti-dienste.de:9443/auth/realms/Service-Authenticate/protocol/openid-connect/token",
                                authorizationEndpoint = "https://fhir-directory-tu.vzd.ti-dienste.de/service-authenticate",
                            ),
                        holder =
                            HolderConfig(
                                apiURL = "https://fhir-directory-tu.vzd.ti-dienste.de/",
                                authorizationEndpoint = "https://fhir-directory-tu.vzd.ti-dienste.de/holder-authenticate",
                            ),
                    ),
                "ru" to
                    EnvironmentConfig(
                        search =
                            SearchConfig(
                                apiURL = "https://fhir-directory-ref.vzd.ti-dienste.de/search",
                            ),
                        fdv =
                            FdvConfig(
                                apiURL = "https://fhir-directory-ref.vzd.ti-dienste.de/fdv/search",
                                authenticationEndpoint = "https://auth-ref.vzd.ti-dienste.de:9443/auth/realms/Service-Authenticate/protocol/openid-connect/token",
                                authorizationEndpoint = "https://fhir-directory-ref.vzd.ti-dienste.de/service-authenticate",
                            ),
                        holder =
                            HolderConfig(
                                apiURL = "https://fhir-directory-ref.vzd.ti-dienste.de/",
                                authorizationEndpoint = "https://fhir-directory-ref.vzd.ti-dienste.de/holder-authenticate",
                            ),
                    ),
                "pu" to
                    EnvironmentConfig(
                        search =
                            SearchConfig(
                                apiURL = "https://fhir-directory.vzd.ti-dienste.de/search",
                            ),
                        fdv =
                            FdvConfig(
                                apiURL = "https://fhir-directory.vzd.ti-dienste.de/fdv/search",
                                authenticationEndpoint = "https://auth.vzd.ti-dienste.de:9443/auth/realms/Service-Authenticate/protocol/openid-connect/token",
                                authorizationEndpoint = "https://fhir-directory.vzd.ti-dienste.de/service-authenticate",
                            ),
                        holder =
                            HolderConfig(
                                apiURL = "https://fhir-directory.vzd.ti-dienste.de/",
                                authorizationEndpoint = "https://fhir-directory.vzd.ti-dienste.de/holder-authenticate",
                            ),
                    ),
            ),
    )

class ConfigException(
    message: String,
    cause: Throwable? = null
) : DirectoryException(message, cause)

@Serializable
data class Config(
    val environments: Map<String, EnvironmentConfig>,
) {
    fun environment(env: DirectoryEnvironment) = environments[env.name] ?: throw ConfigException("Unknown environment: ${env.name}")
}

@Serializable
data class EnvironmentConfig(
    val search: SearchConfig,
    val fdv: FdvConfig,
    val holder: HolderConfig
)

@Serializable
data class SearchConfig(
    val apiURL: String,
)

@Serializable
data class FdvConfig(
    val apiURL: String,
    val authenticationEndpoint: String,
    val authorizationEndpoint: String,
)

@Serializable
data class HolderConfig(
    val apiURL: String,
    val authorizationEndpoint: String,
)
