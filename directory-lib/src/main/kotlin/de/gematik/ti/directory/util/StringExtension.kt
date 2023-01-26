package de.gematik.ti.directory.util

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

fun String.escape(): String = Json.encodeToString(this).replace(Regex("^.|.$"), "")
