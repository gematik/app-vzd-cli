package de.gematik.ti.directory.admin

import com.github.ajalt.clikt.core.CliktError
import kotlinx.coroutines.*
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

    fun tokenize(query: String, tokensToSkip: List<TokenType> = emptyList()): List<Token> {
        val strings = query.split(Regex(" ")).filter { it != "" }.toMutableList()
        val result = mutableListOf<Token>()

        if (!tokensToSkip.contains(TokenType.LocalityName)) {
            consumeLocalityName(strings)?.let {
                result.add(it)
            }
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

/**
 * Experimental search. It is a bit messy, because people might have the same names as cities,
 * thats why we fire 2 queries to the server: one with localityName Token and one without.
 */
suspend fun Client.quickSearch(searchQuery: String): SearchResults {
    var first: SearchResults? = null
    var second: SearchResults? = null


    withContext(Dispatchers.IO) {
        val firstTokens = Tokenizer.tokenize(searchQuery)
        if (firstTokens.firstOrNull { it.type == TokenType.LocalityName } == null) {
            // all safe, not locality found - just do one query
            first = quickSearch(searchQuery, firstTokens)
        } else {
            val secondTokens =  Tokenizer.tokenize(searchQuery, listOf(TokenType.LocalityName))
            listOf(
                launch { first = quickSearch(searchQuery, firstTokens) },
                launch { second = quickSearch(searchQuery, secondTokens) }
            ).joinAll()
        }
    }

    return SearchResults(
        searchQuery = searchQuery,
        directoryEntries = (second?.directoryEntries ?: emptyList()) + (first?.directoryEntries ?: emptyList())
    )

}

suspend fun Client.quickSearch(searchQuery: String, tokens: List<Token>): SearchResults {
    logger.debug { "QuickSearch query: $searchQuery" }

    var reducedQuery = mutableListOf<String>()

    val searchParams = mutableMapOf<String, String>()

    tokens.forEach { token ->
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

    val result = readDirectoryEntry(searchParams + Pair("baseEntryOnly", "true"))
    entries.addAll(result ?: emptyList())

    return SearchResults(searchQuery, entries)
}
