import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val ktorVersion = "2.0.3"
version = "2.0.0-alpha3"

plugins {
    // Apply the org.jetbrains.kotlin.jvm Plugin to add support for Kotlin.
    kotlin("jvm").version("1.6.10")
    kotlin("plugin.serialization").version("1.6.10")
    id("com.github.johnrengelman.shadow").version("7.1.2")
    id("org.jlleitschuh.gradle.ktlint") version "10.2.1"
    // Apply the application plugin to add support for building a CLI application in Java.
    application
    `maven-publish`
}

configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {
    disabledRules.set(setOf("no-wildcard-imports"))
}

repositories {
    // Use Maven Central for resolving dependencies.
    mavenCentral()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "11"
}

dependencies {
    implementation(project(":legacy-client-java"))
    // Align versions of all Kotlin components
    implementation(platform("org.jetbrains.kotlin:kotlin-bom"))
    implementation("org.jetbrains.kotlin:kotlin-stdlib")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("io.github.microutils:kotlin-logging:2.1.21")
    implementation("ch.qos.logback:logback-classic:1.2.9")
    implementation("io.github.cdimascio:dotenv-kotlin:6.2.2")
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-cio:$ktorVersion")
    implementation("io.ktor:ktor-client-logging:$ktorVersion")
    implementation("io.ktor:ktor-client-auth:$ktorVersion")
    implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")
    implementation("net.mamoe.yamlkt:yamlkt:0.10.2")
    implementation("com.google.code.gson:gson:2.9.0")

    implementation("com.github.doyaaaaaken:kotlin-csv-jvm:1.2.0")

    implementation("com.github.ajalt.clikt:clikt:3.4.0")
    implementation("org.bouncycastle:bcprov-jdk15on:1.70")
    implementation("org.bouncycastle:bcpkix-jdk15on:1.70")

    implementation("org.ldaptive:ldaptive:2.1.1")
    implementation("me.tongfei:progressbar:0.9.3")

    // implementation("com.nimbusds:nimbus-jose-jwt:9.23")

    testImplementation("org.jetbrains.kotlin:kotlin-test")
    // testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testImplementation("io.kotest:kotest-runner-junit5:5.1.0")
    testImplementation("io.kotest:kotest-assertions-core:5.1.0")
}

application {
    // Define the main class for the application.
    mainClass.set("de.gematik.ti.directory.CliKt")
}

tasks.register<Zip>("customDist") {
    dependsOn("installShadowDist")
    archiveBaseName.set("${project.name}")
    destinationDirectory.set(layout.buildDirectory.dir("distributions"))
    into("${project.name}-${project.version}/commands") {
        from(layout.projectDirectory.dir("commands"))
    }
    into("${project.name}-${project.version}/") {
        from(layout.buildDirectory.dir("install/vzd-cli-shadow"))
    }
}

tasks.named("build") {
    finalizedBy("customDist")
}

tasks.distZip.configure { enabled = false }
tasks.distTar.configure { enabled = false }
tasks.shadowDistTar.configure { enabled = false }
tasks.shadowDistZip.configure { enabled = false }

tasks.named<JavaExec>("run") {
    standardInput = System.`in`
}

tasks.test {
    useJUnitPlatform()
}

tasks {
    val projectProps by registering(WriteProperties::class) {
        outputFile = file("$buildDir/vzd-cli.properties")
        comment = "vzd-cli BuildConfig"
        property("project.version", project.version)
    }

    processResources {
        from(projectProps)
    }
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = "de.gematik"
            artifactId = "vzd-cli"
            version = version

            from(components["java"])
        }
    }
}
