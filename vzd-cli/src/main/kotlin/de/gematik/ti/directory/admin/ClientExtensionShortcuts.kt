package de.gematik.ti.directory.admin

suspend fun Client.readDirectoryEntryByTelematikID(telematikID: String): DirectoryEntry? {
    return readDirectoryEntry(mapOf("telematikID" to telematikID))?.firstOrNull()
}
