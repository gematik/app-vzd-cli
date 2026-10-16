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

package de.gematik.ti.directory.cli.fhir

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.requireObject
import com.github.ajalt.clikt.parameters.options.option
import de.gematik.ti.directory.cli.catching

class FdvTokenCommand : CliktCommand(name = "token", help = "Set or get Access Token for FDVSearchAPI") {
    private val context by requireObject<FhirCliEnvironmentContext>()
    private val token by option(
        "-s",
        "--set",
        metavar = "ACCESS_TOKEN",
        help = "Sets OAuth2 Access Token",
        envvar = "FDV_SEARCH_ACCESS_TOKEN",
    )

    override fun run() =
        catching {
            token?.let { context.fhirAPI.storeAccessTokenFdv(context.env, it) }

            echo(
                context.fhirAPI.retrieveAccessTokenFdv(context.env),
            )
        }
}
