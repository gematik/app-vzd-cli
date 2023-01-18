package de.gematik.ti.directory.admin

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

private val RE_POSTAL_CODE = Regex("^[0-9]{5}\$")
private val RE_TELEMATIK_ID = Regex("^[0-9]{1,2}-.*$")
private val RE_DOMAIN_ID = Regex("^[0-9]{6,}")

@Serializable
data class SearchResults(
    val searchQuery: String,
    @Contextual
    val directoryEntries: List<DirectoryEntry>,
)

enum class TokenType {
    Plain,
    PostalCode,
    TelematikID,
    LocalityName,
    DomainID,
}

fun String.trailingAsterisk(): String {
    return if (!this.contains("*")) {
        "$this*"
    } else {
        this
    }
}

fun String.leadindAndTrailingAsterisks(): String {
    if (!this.contains("*")) {
        return "*$this*"
    } else {
        return this
    }
}

data class TokenPosition(val type: TokenType, val range: IntRange)

data class TokenizerResult(val tokens: List<String>, val positions: List<TokenPosition>) {
    fun joinAll(): String {
        return tokens.joinToString(" ")
    }

    fun join(tokenPosition: TokenPosition): String {
        return tokens.slice(tokenPosition.range).joinToString(" ")
    }

    fun joinAllExcept(tokenPosition: TokenPosition): String {
        return tokens.filterIndexed { index, _ ->
            index !in tokenPosition.range
        }.joinToString(" ")
    }

    fun subset(subsetPositions: List<TokenPosition>): TokenizerResult {
        val subsetTokens = tokens.filterIndexed { index, _ ->
            subsetPositions.any { index in it.range }
        }
        return TokenizerResult(subsetTokens, subsetPositions)
    }
}

object POSTokenizer {
    private val LOCALITY_NAMES = lazy {
        object {}.javaClass.getResourceAsStream("/PLZ_2021.csv")
            ?.bufferedReader()
            ?.readLines()
            ?.map { it.split(",")[1].lowercase() }
            ?.toSet()
    }

    fun tokenize(query: String): TokenizerResult {
        val tokens = query.split(Regex(" ")).filter { it != "" }

        val positions = mutableListOf<TokenPosition>()

        // look for 3 ,2 and 1 word localities
        listOf(3, 2, 1).forEach { localityLength ->
            if (localityLength > tokens.size) return@forEach
            // try also with one word offset
            listOf(0, 1).forEach { offset ->
                @Suppress("LABEL_NAME_CLASH")
                if (tokens.size < localityLength + offset) return@forEach
                tokens.drop(offset).chunked(localityLength).forEachIndexed { index, strings ->
                    if (strings.size < localityLength) return@forEachIndexed
                    val candidate = strings.joinToString(" ")
                    if (LOCALITY_NAMES.value?.contains(candidate.lowercase()) == true) {
                        val startPos = (index) + offset
                        val endPos = startPos + localityLength
                        // only add if we dont have already position in the same range
                        if (!positions.any { startPos >= it.range.first && endPos-1 <= it.range.last }) {
                            positions.add(
                                TokenPosition(
                                    TokenType.LocalityName,
                                    startPos until endPos,
                                ),
                            )
                        }
                    }
                }
            }
        }

        tokens.forEachIndexed { index, token ->
            if (positions.any { index in it.range }) return@forEachIndexed
            if (RE_POSTAL_CODE.matches(token)) {
                positions.add(TokenPosition(TokenType.PostalCode, index until index + 1))
            } else if (RE_TELEMATIK_ID.matches(token)) {
                positions.add(TokenPosition(TokenType.TelematikID, index until index + 1))
            } else if (RE_DOMAIN_ID.matches(token)) {
                positions.add(TokenPosition(TokenType.DomainID, index until index + 1))
            } else {
                positions.add(TokenPosition(TokenType.Plain, index until index + 1))
            }
        }

        return TokenizerResult(tokens, positions.toList())
    }
}

private fun extractFixedParams(tokenizerResult: TokenizerResult): Pair<Map<String, String>, TokenizerResult> {
    val namesAndLocalities = mutableListOf<TokenPosition>()
    val fixedParams = buildMap<String, String> {
        tokenizerResult.positions.forEach { position ->
            when (position.type) {
                TokenType.TelematikID -> {
                    put("telematikID", tokenizerResult.tokens[position.range.first].trailingAsterisk())
                }
                TokenType.PostalCode -> {
                    put("postalCode", tokenizerResult.tokens[position.range.first])
                }
                TokenType.DomainID -> {
                    put("domainID", tokenizerResult.tokens[position.range.first].trailingAsterisk())
                }
                else -> {
                    namesAndLocalities.add(position)
                }
            }
        }
        put("baseEntryOnly", "true")
    }
    return Pair(fixedParams, tokenizerResult.subset(namesAndLocalities))
}

/**
 * Experimental search. It is a bit messy, because people might have the same names as localities,
 * that is why we fire more than one query to the server: with localityName and without.
 */
suspend fun Client.quickSearch(searchQuery: String): SearchResults {
    logger.debug { "QuickSearch query: $searchQuery" }

    val tokenizerResult = POSTokenizer.tokenize(searchQuery)

    val (fixedParams, namesAndLocalities) = extractFixedParams(tokenizerResult)

    logger.debug { "Detected fixed query parameters: $fixedParams" }
    logger.debug { "Detected names and localities: $namesAndLocalities" }

    val self = this
    val entries = buildList {
        val entriesList = this
        withContext(Dispatchers.IO) {
            if (!namesAndLocalities.positions.any { it.type == TokenType.LocalityName }) {
                // no localities in search query
                self.readDirectoryEntry(
                    buildMap {
                        putAll(fixedParams)
                        if (namesAndLocalities.positions.isNotEmpty()) {
                            put("displayName", namesAndLocalities.joinAll().leadindAndTrailingAsterisks())
                        }
                    },
                )?.apply { addAll(this) }
            } else {
                // for each locality token query the API
                // queries are fired in parallel using co-routines
                buildList {
                    namesAndLocalities.positions.filter { it.type == TokenType.LocalityName }.forEach { localityPos ->
                        add(
                            launch {
                                self.readDirectoryEntry(
                                    buildMap {
                                        putAll(fixedParams)
                                        put("localityName", namesAndLocalities.join(localityPos).trailingAsterisk())
                                        if (namesAndLocalities.positions.size > 1) {
                                            put("displayName", namesAndLocalities.joinAllExcept(localityPos).leadindAndTrailingAsterisks())
                                        }
                                    },
                                )?.apply {
                                    entriesList.addAll(this)
                                }
                            },
                        )
                        // finish with query without localityName
                        if (tokenizerResult.positions.size > 0) {
                            add(
                                launch {
                                    self.readDirectoryEntry(
                                        buildMap {
                                            putAll(fixedParams)
                                            put("displayName", namesAndLocalities.joinAll().leadindAndTrailingAsterisks())
                                        },
                                    )?.apply {
                                        entriesList.addAll(this)
                                    }
                                },
                            )
                        }
                    }
                }.apply {
                    logger.info { "Executing $size Admin API calls" }
                }.joinAll()
            }
        }
    }

    return SearchResults(
        searchQuery = searchQuery,
        directoryEntries = entries,
    )
}
