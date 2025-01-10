/*
 * This file was generated by the Gradle 'init' task.
 */

plugins {
    kotlin("jvm")
    application
    id("io.ktor.plugin")
    kotlin("plugin.serialization")
    id("app.cash.sqldelight") version "2.0.2"
    id("org.jlleitschuh.gradle.ktlint") version "11.6.1"
}

sqldelight {
    databases {
        create("PayloadDatabase") {
            deriveSchemaFromMigrations.set(true)
            migrationOutputDirectory = file("${layout.buildDirectory.get()}/generated/migrations")
            migrationOutputFileFormat = ".sql"
            packageName.set("no.nav.emottak.queries")
            dialect(libs.sqldelight.postgresql.dialect)
        }
    }
}

tasks {
    shadowJar {
        archiveFileName.set("app.jar")
    }
    test {
        useJUnitPlatform()
    }
    ktlintFormat {
        this.enabled = true
    }
    ktlintCheck {
        dependsOn("ktlintFormat")
    }
    ktlint {
        filter {
            exclude { it.file.path.contains("/generated/") }
            exclude { it.file.path.contains("\\generated\\") }
        }
    }
    compileKotlin.configure {
        // sqldelight task generateMainPayloadDatabaseMigrations will output .sqm files as valid SQL
        // in the output directory, with the output format.
        // Create a dependency from compileKotlin where flyway will have the files available on the classpath
        dependsOn("generateMainPayloadDatabaseMigrations")
    }
    build {
        dependsOn("ktlintCheck")
    }
}

dependencies {
    implementation(project(":felles")) {
        exclude(group = "no.nav.emottak", module = "emottak-payload-xsd")
    }
    implementation(libs.arrow.core)
    implementation(libs.arrow.fx.coroutines)
    implementation(libs.arrow.resilience)
    implementation(libs.arrow.suspendapp)
    implementation(libs.arrow.suspendapp.ktor)
    implementation(libs.flyway.core)
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.bundles.logging)
    implementation(libs.bundles.prometheus)
    implementation(libs.hikari)
    implementation(libs.kotlin.kafka)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.cio)
    implementation(libs.bundles.jakarta.mail)
    implementation(libs.jsch)
    implementation(libs.ktor.client.auth)
    implementation(libs.ktor.server.auth.jvm)
    implementation(libs.token.validation.ktor.v2)
    implementation(libs.hoplite.core)
    implementation(libs.hoplite.hocon)
    implementation(libs.postgresql)
    implementation(libs.sqldelight.jdbc.driver)
    implementation(libs.vault.jdbc)
    runtimeOnly("net.java.dev.jna:jna:5.12.1")
    testRuntimeOnly(testLibs.junit.jupiter.engine)
    testImplementation(testLibs.mockk.jvm)
    testImplementation(testLibs.mockk.dsl.jvm)
    testImplementation(testLibs.junit.jupiter.api)
    testImplementation(testLibs.bundles.kotest)
    testImplementation(testLibs.ktor.server.test.host)
    testImplementation(testLibs.testcontainers)
    testImplementation(testLibs.testcontainers.kafka)
    testImplementation(testLibs.testcontainers.postgresql)
    testImplementation(testLibs.kotest.extensions.testcontainers)
    testImplementation(testLibs.kotest.extensions.testcontainers.kafka)
    testImplementation(testLibs.kotest.assertions.arrow)
    testImplementation(testLibs.postgresql)
    testImplementation(testLibs.turbine)
    testImplementation("com.icegreen:greenmail:2.1.0-alpha-3")
    testImplementation("com.icegreen:greenmail-junit5:2.1.0-alpha-3")
    testImplementation(kotlin("test"))
    implementation(kotlin("stdlib-jdk8"))
}

application {
    mainClass.set("no.nav.emottak.AppKt")
}
