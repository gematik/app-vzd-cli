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

package de.gematik.ti.directory.cli.admin

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.UsageError
import com.github.ajalt.clikt.output.TermUi
import com.github.ajalt.clikt.parameters.groups.OptionGroup
import com.github.ajalt.clikt.parameters.options.option
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class CertificateInputOptions : OptionGroup(name = "Certificate input") {
    val passin: String? by option(
        "--passin",
        metavar = "pass:PW|env:VAR|file:PATH",
        help = "Password source for PKCS#12 files (like openssl)",
    )

    fun passwordProvider(
        command: CliktCommand,
        noPrompt: Boolean,
    ): () -> CharArray? =
        {
            passin?.let { parsePassin(it, System::getenv) { path -> Files.readString(path) } }
                ?: if (noPrompt || System.console() == null) {
                    null
                } else {
                    TermUi.prompt("PKCS#12 password", hideInput = true, console = command.currentContext.console)?.toCharArray()
                }
        }
}

fun parsePassin(
    spec: String,
    env: (String) -> String?,
    readFile: (Path) -> String,
): CharArray {
    val separator = spec.indexOf(':')
    if (separator < 0) throw UsageError("--passin must be pass:PW, env:VAR or file:PATH")
    val value = spec.substring(separator + 1)
    return when (spec.substring(0, separator)) {
        "pass" -> value
        "env" -> env(value) ?: throw UsageError("Environment variable '$value' given in --passin is not set")
        "file" -> readFile(Paths.get(value)).lineSequence().first()
        else -> throw UsageError("--passin must be pass:PW, env:VAR or file:PATH")
    }.toCharArray()
}
