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
    implementation("no.nav.emottak:emottak-utils:0.0.4")
    implementation("com.sun.xml.messaging.saaj:saaj-impl:3.0.2")
    implementation(libs.ebxml.processing.model)
    implementation(libs.bundles.prometheus)
    implementation(libs.jakarta.xml.bind.api)
    implementation(libs.ktor.client.cio)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.netty)
    runtimeOnly("net.java.dev.jna:jna:5.12.1")
    testImplementation(libs.apache.santuario)
    testImplementation(testLibs.junit.jupiter.api)
    testImplementation(testLibs.ktor.server.test.host)
    testImplementation(testLibs.mock.oauth2.server)
    testImplementation(testLibs.mockk.dsl.jvm)
    testImplementation(testLibs.mockk.jvm)
    testRuntimeOnly(testLibs.junit.jupiter.engine)
}

application {
    mainClass.set("no.nav.emottak.ebms.AppKt")
}
