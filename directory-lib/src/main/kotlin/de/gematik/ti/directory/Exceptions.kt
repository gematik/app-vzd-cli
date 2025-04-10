package de.gematik.ti.directory

open class DirectoryException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)

class DirectoryAuthException(
    message: String,
    cause: Throwable? = null
) : DirectoryException(message, cause)
