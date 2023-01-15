package de.gematik.ti.directory.bff.dev

import io.github.cdimascio.dotenv.dotenv

fun main() {
    val dotenv = dotenv { }
    dotenv.entries().forEach { System.setProperty(it.key, it.value) }
    System.setProperty("io.ktor.development", "true")
    de.gematik.ti.directory.bff.main()
}
