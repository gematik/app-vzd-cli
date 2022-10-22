package de.gematik.ti.directory.util

open class GenericDirectoryExceptionException(message: String, cause: Throwable? = null) : Exception(message, cause)

class DirectoryAuthException(message: String, cause: Throwable? = null) : GenericDirectoryExceptionException(message, cause)
