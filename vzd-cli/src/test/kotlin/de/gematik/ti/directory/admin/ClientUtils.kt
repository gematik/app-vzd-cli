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
