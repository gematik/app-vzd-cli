package de.gematik.ti.directory.admin

import de.gematik.ti.directory.cli.Cli
import io.kotest.core.test.TestScope
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.PrintStream

fun runCLI(args: List<String>, data: String? = null): String {
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

fun TestScope.cli(vararg args: String, test: (output: String) -> Unit) {
    test(runCLI(args.asList()))
}
