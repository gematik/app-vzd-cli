package de.gematik.ti.directory.validator

enum class FindingType(severity: Severity, title: String) {
    TELEMATIK_ID_INVALID_FORMAT(Severity.CRITICAL, "TelematikID hat ungültiges Format"),
    HOLDER_IS_EMPTY(Severity.ERROR, "Attribut `holder` ist leer"),
    DISPLAYNAME_IS_EMPTY(Severity.CRITICAL, "Attribut `displayName` ist leer"),
}