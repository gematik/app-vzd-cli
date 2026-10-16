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
