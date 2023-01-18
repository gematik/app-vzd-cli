package de.gematik.ti.directory.elaborate

import kotlinx.serialization.Serializable

@Serializable
data class ValidationError(
    val dataPath: String,
    val message: String,
)

@Serializable
data class ValidationResult(
    val errors: List<ValidationError>,
)
