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
    val templateValues: List<String>? = null,
)

@Serializable
data class ValidationResult(
    val findings: List<Finding>? = null,
    val attributes: Map<String, ValidationResult>? = null,
)