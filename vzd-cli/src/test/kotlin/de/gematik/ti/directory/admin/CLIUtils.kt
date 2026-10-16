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

import de.gematik.ti.directory.cli.Cli
import io.kotest.core.test.TestScope
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.PrintStream

fun runCLI(
    args: List<String>,
    data: String? = null,
): String {
    val stdin = System.`in`
    val stdout = System.`out`
    try {
        data?.let { System.setIn(ByteArrayInputStream(it.toByteArray(Charsets.UTF_8))) }
        val output = ByteArrayOutputStream()
        System.setOut(PrintStream(output))
        Cli().parse(args)
        return output.toString(Charsets.UTF_8)
    } finally {
        System.setOut(stdout)
        System.setIn(stdin)
    }
}

fun TestScope.cli(
    vararg args: String,
    test: (output: String) -> Unit,
) {
    test(runCLI(args.asList()))
}
