

plugins {
    // Apply the common convention plugin for shared build configuration between library and application projects.
    id("de.gematik.directory.common-conventions")
    id("org.jlleitschuh.gradle.ktlint")
    // Apply the java-library plugin for API and implementation separation.
    `java-library`
}
