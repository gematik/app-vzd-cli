package de.gematik.ti.directory.validation

import kotlinx.serialization.Serializable

@Serializable
enum class FindingSeverity {
    INFO,
    WARNING,
    ERROR
}

@Serializable
data class Finding(
    val code: String,
    val severity: FindingSeverity,
    val index: Int? = null,
    val key: String? = null,
)
