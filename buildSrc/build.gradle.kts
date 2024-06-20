plugins {
    // Support convention plugins written in Kotlin. Convention plugins are build scripts in 'src/main' that automatically become available as plugins in the main build.
    base
    kotlin("jvm") version "1.9.22" apply false
    `kotlin-dsl`
    kotlin("plugin.serialization") version "1.9.22" apply false
    id("org.jlleitschuh.gradle.ktlint") version "11.6.0" apply false
}

repositories {
    // Use the plugin portal to apply community plugins in convention plugins.
    gradlePluginPortal()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:1.9.22")
}
