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

package de.gematik.ti.directory.cli.fhir

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.CliktError
import com.github.ajalt.clikt.core.requireObject
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.prompt
import de.gematik.ti.directory.cli.catching

class FdvLoginCommand : CliktCommand(name = "login", help = "Login to FDVSearchAPI") {
    private val context by requireObject<FhirCliEnvironmentContext>()
    private val password by option(
        "--password",
        "-p",
        help = "Password for protection of the Vault",
        envvar = "VAULT_PASSWORD",
    ).prompt("Enter Vault Password", hideInput = true)

    override fun run() =
        catching {
            val vault = context.fhirAPI.openVaultFdv(password)
            val credentials = vault.get(context.env.name) ?: throw CliktError("No credentials found for ${context.env.name}")
            context.fhirAPI.loginFdv(context.env, credentials.name, credentials.secret)
            echo("Logged in as ${credentials.name} to FHIR FDV API (${context.env.name})")
        }
}
