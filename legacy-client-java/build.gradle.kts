plugins {
    `java-library`
}

repositories {
    // Use Maven Central for resolving dependencies.
    mavenCentral()
}

dependencies {
    api("jakarta.xml.bind:jakarta.xml.bind-api:4.0.0")
    implementation("com.sun.xml.bind:jaxb-impl:4.0.0")
    implementation("org.apache.commons:commons-lang3:3.12.0")
    implementation("org.apache.oltu.oauth2:org.apache.oltu.oauth2.client:1.0.2")
    implementation("org.apache.oltu.oauth2:org.apache.oltu.oauth2.common:1.0.2")
    implementation("org.apache.httpcomponents:httpclient:4.5.13")
    implementation("com.squareup.okhttp3:okhttp:4.9.3")
    implementation("com.squareup.okhttp3:logging-interceptor:4.9.3")
    implementation("io.gsonfire:gson-fire:1.8.5")
    implementation("io.swagger:swagger-annotations:1.6.6")
    implementation("commons-io:commons-io:2.5")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.9.8")
    implementation("com.google.code.findbugs:jsr305:3.0.2")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.mockito:mockito-core:4.5.1")
}


tasks.withType<JavaCompile> {
    sourceCompatibility = "11"
    targetCompatibility = "11"
//    options.compilerArgs.add("-Xlint:deprecation")
}

tasks.test {
    include("**/UnitTestsuite.class")
    include("**/IntegrationTestsuite.class")
}

