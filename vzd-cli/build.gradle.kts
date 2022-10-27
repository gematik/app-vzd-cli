
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

version = "2.1.0a2"

val ktorVersion = "2.1.2"
val kotestVersion = "5.5.2"

plugins {
    id("org.jetbrains.kotlin.jvm") version "1.7.20"
    kotlin("plugin.serialization").version("1.7.20")
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
    implementation("org.apache.opennlp:opennlp-tools:2.0.0")
    implementation("org.apache.opennlp:opennlp-uima:2.0.0")

    implementation(project(":legacy-client-java"))
    // Align versions of all Kotlin components
    implementation(platform("org.jetbrains.kotlin:kotlin-bom"))
    implementation("org.jetbrains.kotlin:kotlin-stdlib")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("io.github.microutils:kotlin-logging:2.1.21")
    implementation("ch.qos.logback:logback-classic:1.2.9")
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-cio:$ktorVersion")
    implementation("io.ktor:ktor-client-logging:$ktorVersion")
    implementation("io.ktor:ktor-client-auth:$ktorVersion")
    implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")
    implementation("net.mamoe.yamlkt:yamlkt:0.12.0")
    implementation("com.google.code.gson:gson:2.9.0")

    // server
    implementation("io.ktor:ktor-server-core:$ktorVersion")
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("io.ktor:ktor-server-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-server-resources:$ktorVersion")
    implementation("io.ktor:ktor-server-status-pages:$ktorVersion")
    implementation("io.ktor:ktor-server-sessions:$ktorVersion")
    implementation("io.ktor:ktor-server-auth:$ktorVersion")

    implementation("com.github.doyaaaaaken:kotlin-csv-jvm:1.6.0")

    implementation("com.github.ajalt.clikt:clikt:3.4.0")
    implementation("org.bouncycastle:bcprov-jdk15on:1.70")
    implementation("org.bouncycastle:bcpkix-jdk15on:1.70")

    implementation("org.ldaptive:ldaptive:2.1.1")
    implementation("me.tongfei:progressbar:0.9.3")
    implementation("hu.vissy.plain-text-table:ptt-kotlin:1.1.7")
    implementation("hu.vissy.plain-text-table:ptt-core:3.0.0")

    testImplementation("org.jetbrains.kotlin:kotlin-test")
    testImplementation("io.kotest:kotest-runner-junit5:$kotestVersion")
    testImplementation("io.kotest:kotest-assertions-core:$kotestVersion")
}

application {
    // Define the main class for the application.
    mainClass.set("de.gematik.ti.directory.cli.CliKt")
}

tasks.register<Zip>("customDist") {
    dependsOn("installShadowDist")
    archiveBaseName.set(project.name)
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

tasks.register<JavaExec>("serve") {
    mainClass.set("de.gematik.ti.directory.bff.TestServerKt")
    // mainClass.set("de.gematik.ti.directory.cli.CliKt")
    // args = listOf("gui")
    classpath = sourceSets["test"].runtimeClasspath
    jvmArgs = listOf("-Dio.ktor.development=true")
}
