package de.gematik.ti.directory.bff

import de.gematik.ti.directory.admin.BaseDirectoryEntry
import de.gematik.ti.directory.admin.DirectoryEntry
import de.gematik.ti.directory.admin.quickSearch
import de.gematik.ti.directory.elaborate.ElaborateDirectoryEntry
import de.gematik.ti.directory.elaborate.elaborate
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable

@Serializable
data class SearchResultsRepresentation(
    val searchQuery: String,
    val directoryEntries: List<ElaborateDirectoryEntry>,
)

@Serializable
data class DirectoryEntrySearchRepresentation(
    val base: BaseDirectoryEntry,
) {
    companion object {
        fun from(entry: DirectoryEntry): DirectoryEntrySearchRepresentation {
            return DirectoryEntrySearchRepresentation(
                base = entry.directoryEntryBase,
            ).let {
                // todo: hide meta from end users?
                it.base.meta = null
                it
            }
        }
    }
}

fun Route.searchRoute() {
    get("search") {
        val searchQuery = call.request.queryParameters["q"] ?: throw BadRequestException()
        val searchResult = call.adminClient.quickSearch(searchQuery)

        call.respond(
            SearchResultsRepresentation(
                searchQuery = searchQuery,
                searchResult.directoryEntries.map { it.elaborate() },
            ),
        )
    }
}
