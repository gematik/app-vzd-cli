package de.gematik.ti.directory.bff

import io.ktor.http.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ErrorResponse(
    val error: String,
    @SerialName("error_description")
    val errorDescription: String,
)

open class RequestException(
    val code: HttpStatusCode,
    val error: String,
    val errorDescription: String,
) : Exception() {
    fun response(): ErrorResponse {
        return ErrorResponse(error, errorDescription)
    }
}

class BadRequestException : RequestException(HttpStatusCode.BadRequest, "bad_request", "Bad Request")
