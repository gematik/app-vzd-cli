package vzd.teststuite.admin

import vzd.Cli
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
