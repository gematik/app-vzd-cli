plugins {
    // Apply the common convention plugin for shared build configuration between library and application projects.
    id("de.gematik.directory.common-conventions")

    // Apply the application plugin to add support for building a CLI application in Java.
    application
}
