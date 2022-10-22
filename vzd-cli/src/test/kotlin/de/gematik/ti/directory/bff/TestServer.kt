package de.gematik.ti.directory.bff

import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.resources.*

fun main() {
    val port = 57036

    embeddedServer(Netty, port = port, host = "127.0.0.1") {
        directoryModule()
    }.start(wait = true)
}
