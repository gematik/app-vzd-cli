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

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import de.gematik.ti.directory.cli.GlobalAPI
import de.gematik.ti.directory.cli.admin.AdminAPI
import de.gematik.ti.directory.cli.admin.AdminEnvironment
import org.slf4j.LoggerFactory

fun createClient(): Client {
    val root: Logger = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME) as Logger
    root.level = Level.DEBUG

    val adminAPI = AdminAPI(GlobalAPI())
    val tucfg = adminAPI.config.environment(AdminEnvironment.tu)

    return Client {
        apiURL = tucfg.apiURL
        auth {
            accessToken {
                System.getenv()["TEST_ACCESS_TOKEN"] ?: throw RuntimeException("Environment variable 'TEST_ACCESS_TOKEN' must be set.")
            }
        }
        if (adminAPI.globalAPI.config.httpProxy.enabled) {
            httpProxyURL = adminAPI.globalAPI.config.httpProxy.proxyURL
        }
    }
}
