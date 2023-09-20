plugins {
    id("de.gematik.directory.app-conventions")
    kotlin("plugin.serialization") version "1.8.20"
    id("com.github.johnrengelman.shadow") version "8.1.1"
    id("org.jlleitschuh.gradle.ktlint") version "11.6.0"
    id("io.ktor.plugin") version "2.2.2"
}

configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {
    disabledRules.set(setOf("no-wildcard-imports"))
}

repositories {
    gradlePluginPortal()
    mavenCentral()
}

dependencies {
    implementation(project(":directory-lib"))
    // Ktor server (for GUI BFF)
    val ktorVersion: String by project
    implementation("io.ktor:ktor-server-core:$ktorVersion")
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("io.ktor:ktor-server-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-server-status-pages:$ktorVersion")
    // ktor OpenAPI generator
    // implementation("io.github.smiley4:ktor-swagger-ui:1.0.1")
    // use dotenv only for testing
    testImplementation("io.github.cdimascio:dotenv-kotlin:6.4.0")
    // test host for ktor
    testImplementation("io.ktor:ktor-server-test-host:$ktorVersion")
}

application {
    mainClass.set("de.gematik.ti.directory.bff.AppKt")
}

tasks {
    val projectProps by registering(WriteProperties::class) {
        outputFile = file("$buildDir/de.gematik.ti.directory.bff.properties")
        comment = "BuildConfig"
        property("project.version", project.version)
    }

    processResources {
        from(projectProps)
    }
}

tasks.named<JavaExec>("run") {
    jvmArgs = listOf("-Dio.ktor.development=true")
}

tasks.register<JavaExec>("serve") {
    mainClass.set("de.gematik.ti.directory.bff.dev.DevServerKt")
    classpath = sourceSets["test"].runtimeClasspath
}
