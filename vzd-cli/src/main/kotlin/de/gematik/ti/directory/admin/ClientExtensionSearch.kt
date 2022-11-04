package de.gematik.ti.directory.admin

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

private val RE_POSTAL_CODE = Regex("^[0-9]{5}\$")
private val RE_TELEMATIK_ID = Regex("^[0-9]{1,2}-.*$")
private val RE_DOMAIN_ID = Regex("^[0-9]{6,}")

@Serializable
data class SearchResults(
    val searchQuery: String,
    @Contextual
    val directoryEntries: List<DirectoryEntry>
)

enum class TokenType {
    Plain,
    PostalCode,
    TelematikID,
    LocalityName,
    DomainID;
}

fun String.trailingAsterisk(): String {
    if (!this.contains("*")) {
        return "$this*"
    } else {
        return this
    }
}

fun String.asterisks(): String {
    if (!this.contains("*")) {
        return "*$this*"
    } else {
        return this
    }
}

class Token(val value: String, val type: TokenType)

object Tokenizer {
    val LOCALITY_NAMES = lazy {
        object {}.javaClass.getResourceAsStream("/PLZ_2021.csv")
            ?.bufferedReader()
            ?.readLines()
            ?.map { it.split(",")[1].lowercase() }
            ?.toSet()
    }

    fun tokenize(query: String): List<Token> {
        val strings = query.split(Regex(" ")).filter { it != "" }.toMutableList()
        val result = mutableListOf<Token>()

        consumeLocalityName(strings)?.let {
            result.add(it)
        }

        strings.forEach { str ->
            if (RE_POSTAL_CODE.matches(str)) {
                result.add(Token(str, TokenType.PostalCode))
            } else if (RE_TELEMATIK_ID.matches(str)) {
                result.add(Token(str, TokenType.TelematikID))
            } else if (RE_DOMAIN_ID.matches(str)) {
                result.add(Token(str, TokenType.DomainID))
            } else {
                result.add(Token(str, TokenType.Plain))
            }
        }

        return result
    }

    fun consumeLocalityName(strings: MutableList<String>, start: Int, length: Int): Token? {
        if (strings.size >= start + length) {
            var position = start
            strings.slice(position..strings.size - 1).chunked(length).forEach() {
                val candidate = it.joinToString(" ")
                if (LOCALITY_NAMES.value?.contains(candidate.lowercase()) == true) {
                    var endPos = position + length
                    if (endPos > strings.size) {
                        endPos = strings.size
                    }
                    strings.subList(position, endPos).clear()
                    return Token(candidate, TokenType.LocalityName)
                }
                position += length
            }
        }
        return null
    }

    fun consumeLocalityName(strings: MutableList<String>): Token? {
        consumeLocalityName(strings, 0, 2)?.let {
            return it
        }
        consumeLocalityName(strings, 0, 3)?.let {
            return it
        }
        // shift to position 1 and try all odd strings
        consumeLocalityName(strings, 1, 2)?.let {
            return it
        }
        consumeLocalityName(strings, 1, 3)?.let {
            return it
        }
        // final try with one word localities
        consumeLocalityName(strings, 0, 1)?.let {
            return it
        }

        return null
    }
}

suspend fun Client.quickSearch(queryString: String): SearchResults {
    logger.debug { "QuickSearch query: $queryString" }

    var reducedQuery = mutableListOf<String>()

    val searchParams = mutableMapOf<String, String>()

    Tokenizer.tokenize(queryString).forEach { token ->
        when (token.type) {
            TokenType.TelematikID -> {
                searchParams["telematikID"] = token.value.trailingAsterisk()
            }
            TokenType.PostalCode -> {
                searchParams["postalCode"] = token.value
            }
            TokenType.DomainID -> {
                searchParams["domainID"] = token.value.trailingAsterisk()
            }
            TokenType.LocalityName -> {
                searchParams["localityName"] = token.value.trailingAsterisk()
            }
            TokenType.Plain -> {
                reducedQuery.add(token.value)
            }
        }
    }

    if (reducedQuery.isNotEmpty()) {
        searchParams["displayName"] = reducedQuery.joinToString(" ").asterisks()
    }

    logger.debug { searchParams }

    val entries = mutableListOf<DirectoryEntry>()

    val result = readDirectoryEntryV2(searchParams + Pair("baseEntryOnly", "true"), 100)
    entries.addAll(result?.directoryEntries ?: emptyList())

    return SearchResults(queryString, entries)
}
