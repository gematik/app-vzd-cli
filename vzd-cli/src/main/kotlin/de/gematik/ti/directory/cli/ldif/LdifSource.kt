package de.gematik.ti.directory.cli.ldif

import org.ldaptive.LdapEntry
import org.ldaptive.io.LdifReader
import java.io.BufferedReader
import java.io.StringReader
import java.util.*

private val BINARY_ENTRY_REGEX = "(.*)::\\s(.*)".toRegex()

class LdifSource(private val input: BufferedReader) {

    fun useEntries(block: (LdapEntry, Long) -> Unit) {
        var line = input.readLine()
        val linesBuffer = mutableListOf<String>()

        while (line != null) {
            BINARY_ENTRY_REGEX.find(line)?.let { match ->
                val encoded = match.groups[2]?.value
                val fixed = decodable(encoded) ?: run { decodable("$encoded=") }
                line = "${match.groups[1]?.value}:: $fixed"
            }

            if (line == "") {
                val entryString = StringReader(linesBuffer.joinToString("\n"))
                linesBuffer.clear()
                val ldifReader = LdifReader(entryString)
                val result = ldifReader.read()
                block(result.entry, entryString.toString().length.toLong() + 1L)
            }
            linesBuffer.add(line)
            line = input.readLine()
        }
    }

    private fun decodable(encoded: String?): String? {
        if (encoded == null) {
            return null
        }
        return try {
            Base64.getDecoder().decode(encoded)
            encoded
        } catch (e: java.lang.IllegalArgumentException) {
            null
        }
    }
}
