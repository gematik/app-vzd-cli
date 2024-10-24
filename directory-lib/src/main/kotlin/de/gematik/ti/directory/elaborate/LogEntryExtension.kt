package de.gematik.ti.directory.elaborate

import de.gematik.ti.directory.admin.LogEntry

fun LogEntry.elaborate(): ElaborateLogEntry {
    val log = this
    return ElaborateLogEntry(
        clientID = log.clientID,
        logTime = log.logTime,
        operation = log.operation,
        noDataChanged = log.noDataChanged,
    )
}