
plugins {
    id("de.gematik.directory.library-conventions")
    id("org.jlleitschuh.gradle.ktlint") version "11.0.0"
    kotlin("plugin.serialization") version "1.7.10"
}

dependencies {
    // Proper Dates support
    api("org.jetbrains.kotlinx:kotlinx-datetime:0.4.0")
    // Kotlin logging wrapper
    api("io.github.microutils:kotlin-logging:3.0.4")
    // logback logging backend
    api("ch.qos.logback:logback-classic:1.4.5")
    // Ktor client libraties
    val ktorVersion: String by project
    api("io.ktor:ktor-client-core:$ktorVersion")
    api("io.ktor:ktor-client-cio:$ktorVersion")
    api("io.ktor:ktor-client-logging:$ktorVersion")
    api("io.ktor:ktor-client-auth:$ktorVersion")
    api("io.ktor:ktor-client-content-negotiation:$ktorVersion")
    api("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")
    // Bouncy castle fpr crypto and certificates processing
    val bcVersion: String by project
    api("org.bouncycastle:bcprov-jdk15on:$bcVersion")
    api("org.bouncycastle:bcpkix-jdk15on:$bcVersion")
    // HAPI-FHIR Model classes
    val hapiVersion: String by project
    api("ca.uhn.hapi.fhir:hapi-fhir-base:$hapiVersion")
    api("ca.uhn.hapi.fhir:hapi-fhir-structures-r4:$hapiVersion")
}

configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {
    disabledRules.set(setOf("no-wildcard-imports"))
}
