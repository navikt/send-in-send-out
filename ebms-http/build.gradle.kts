/*
 * This file was generated by the Gradle 'init' task.
 */

plugins {
    kotlin("jvm")
    application
    id("io.ktor.plugin")
    kotlin("plugin.serialization") apply true
    id("org.jlleitschuh.gradle.ktlint") version "11.6.1"
}

tasks {
    shadowJar {
        archiveFileName.set("app.jar")
    }
}

tasks.register<Wrapper>("wrapper") {
    gradleVersion = "8.1.1"
}

tasks.test {
    useJUnitPlatform()
}

tasks {

    ktlintFormat {
        this.enabled = true
    }
    ktlintCheck {
        dependsOn("ktlintFormat")
    }

    build {
        dependsOn("ktlintCheck")
    }
}

dependencies {
    implementation(project(":felles"))
    implementation("io.ktor:ktor-server-call-logging-jvm:2.3.4")
    implementation("io.ktor:ktor-server-core-jvm:2.3.4")
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.cio)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.bundles.prometheus)
    implementation("no.nav.emottak:ebxml-processing-model:2024102305ad1b37a7c4ec36bc77ef91f5ff67098389922f")
    implementation(libs.jakarta.xml.bind.api)
    implementation("com.sun.xml.messaging.saaj:saaj-impl:3.0.2")
    runtimeOnly("net.java.dev.jna:jna:5.12.1")
    testImplementation(testLibs.mock.oauth2.server)
    testImplementation(testLibs.ktor.server.test.host)
    testImplementation(testLibs.junit.jupiter.api)
    testImplementation(testLibs.mockk.jvm)
    testImplementation(testLibs.mockk.dsl.jvm)
    testImplementation(libs.apache.santuario)
    // testImplementation(testLibs.mockk.jvm)
    testRuntimeOnly(testLibs.junit.jupiter.engine)
}

application {
    mainClass.set("no.nav.emottak.ebms.AppKt")
}
/*
ktlint {
    ignoreFailures.set(false)
}
 */
