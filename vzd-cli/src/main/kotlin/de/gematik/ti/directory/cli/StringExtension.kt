package de.gematik.ti.directory.cli

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

fun String.escape(): String = Json.encodeToString(this).replace(Regex("^.|.$"), "")