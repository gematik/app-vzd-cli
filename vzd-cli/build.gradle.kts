import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

val bcVersion: String by project

plugins {
    id("de.gematik.directory.app-conventions")
    kotlin("plugin.serialization") version "1.8.10"
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

dependencies {
    implementation(project(":directory-lib"))
    // Align versions of all Kotlin components
    implementation(platform("org.jetbrains.kotlin:kotlin-bom"))
    implementation("org.jetbrains.kotlin:kotlin-stdlib")

    // implementation("org.jetbrains.kotlin:kotlin-reflect")
    // Kotlin logging wrapper
    // implementation("io.github.microutils:kotlin-logging:3.0.4")
    // logback logging backend
    // implementation("ch.qos.logback:logback-classic:1.4.5")
    // Ktor client libraties
    // val ktorVersion: String by project
    // implementation("io.ktor:ktor-client-core:$ktorVersion")
    // implementation("io.ktor:ktor-client-cio:$ktorVersion")
    // implementation("io.ktor:ktor-client-logging:$ktorVersion")
    // implementation("io.ktor:ktor-client-auth:$ktorVersion")
    // implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
    // implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")
    // Proper Dates support
    // implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.4.0")

    // YAML support for kotlinx serialisation
    implementation("net.mamoe.yamlkt:yamlkt:0.12.0")

    // CSV support
    implementation("com.github.doyaaaaaken:kotlin-csv-jvm:1.7.0")

    // HAPI-FHIR Model classes
    // val hapiVersion: String by project
    // implementation("ca.uhn.hapi.fhir:hapi-fhir-base:$hapiVersion")
    // implementation("ca.uhn.hapi.fhir:hapi-fhir-structures-r4:$hapiVersion")

    // LDAP/LDIF processing
    // implementation("org.ldaptive:ldaptive:2.1.1")

    // Text-GUI: Progressbar
    implementation("me.tongfei:progressbar:0.9.5")
    // Text-GUI: Text Tables
    implementation("hu.vissy.plain-text-table:ptt-kotlin:1.1.7")
    implementation("hu.vissy.plain-text-table:ptt-core:3.0.0")

    // OpenNLP - natural text prcessor
    // implementation("org.apache.opennlp:opennlp-tools:2.0.0")
    // implementation("org.apache.opennlp:opennlp-uima:2.0.0")

    // Ktor server (for GUI BFF)
    val ktorVersion: String by project
    implementation("io.ktor:ktor-server-core:$ktorVersion")
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("io.ktor:ktor-server-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-server-resources:$ktorVersion")
    implementation("io.ktor:ktor-server-status-pages:$ktorVersion")
    implementation("io.ktor:ktor-server-sessions:$ktorVersion")
    implementation("io.ktor:ktor-server-auth:$ktorVersion")

    // Clikt library for creating nice CLI
    implementation("com.github.ajalt.clikt:clikt:3.5.1")

    // Validation framework
    implementation("io.konform:konform-jvm:0.4.0")

    // Bouncy castle fpr crypto and certificates processing
    shadow("org.bouncycastle:bcprov-jdk15on:$bcVersion")
    shadow("org.bouncycastle:bcpkix-jdk15on:$bcVersion")

    // bouncy castle again, just for unit tests
    testImplementation("org.bouncycastle:bcprov-jdk15on:$bcVersion")
    testImplementation("org.bouncycastle:bcpkix-jdk15on:$bcVersion")
}

application {
    // Define the main class for the application.
    mainClass.set("de.gematik.ti.directory.cli.CliKt")
}

tasks.register<Zip>("customDist") {
    dependsOn("installShadowDist")
    archiveBaseName.set(project.name)
    destinationDirectory.set(layout.buildDirectory.dir("distributions"))

    into("${project.name}-${project.version}/bin") {
        from(layout.projectDirectory.dir("additionalScripts"))
    }

    into("${project.name}-${project.version}/") {
        from(layout.buildDirectory.dir("install/vzd-cli-shadow"))
    }
}

tasks.named("build") {
    finalizedBy("customDist")
}

tasks.named("startScripts") {
    dependsOn("copyShadowLibs")
}

tasks.named<ShadowJar>("shadowJar") {
    exclude("org/bouncycastle/**")
    exclude("META-INF/versions/**")
    exclude("META-INF/java.security.Provider")
    exclude("BC*")
    dependsOn("copyShadowLibs")
    archiveVersion.set("")
}

tasks.register<Copy>("copyShadowLibs") {
    from(configurations.shadow)
    into(layout.buildDirectory.dir("libs"))
}

tasks.named<CreateStartScripts>("startShadowScripts") {
    val generator = windowsStartScriptGenerator as TemplateBasedScriptGenerator
    generator.template = project.resources.text.fromFile("startScriptTemplates/windowsStartScript.txt")
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
        destinationFile.set(File(layout.buildDirectory.asFile.get(), "vzd-cli.properties"))
        comment = "vzd-cli BuildConfig"
        property("project.version", project.version)
    }

    processResources {
        from(projectProps)
    }
}

tasks.register<JavaExec>("serve") {
    mainClass.set("de.gematik.ti.directory.bff.TestServerKt")
    // mainClass.set("de.gematik.ti.directory.cli.CliKt")
    // args = listOf("gui")
    classpath = sourceSets["test"].runtimeClasspath
    jvmArgs = listOf("-Dio.ktor.development=true")
}

tasks.named<JavaExec>("run") {
    classpath = sourceSets["test"].runtimeClasspath
}
