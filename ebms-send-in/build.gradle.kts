import com.github.jengelman.gradle.plugins.shadow.transformers.AppendingTransformer
import org.gradle.kotlin.dsl.testLibs

/*
 * This file was generated by the Gradle 'init' task.
 */

plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    application
    id("io.ktor.plugin")
    id("org.jlleitschuh.gradle.ktlint") version "11.6.1"
}

tasks {
    shadowJar {
        mergeServiceFiles()
        transform(AppendingTransformer::class.java) {
            this.resource = "META-INF/cxf/bus-extensions.txt"
        }
        archiveFileName.set("app.jar")
    }
    register<Wrapper>("wrapper") {
        gradleVersion = "8.1.1"
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
    build {
        dependsOn("ktlintCheck")
    }
}

dependencies {
    implementation(project(":felles"))
    implementation("no.nav.emottak:emottak-utils:0.0.7")
    implementation("com.sun.xml.messaging.saaj:saaj-impl:1.5.1")
    implementation(libs.hoplite.core)
    implementation(libs.hoplite.hocon)
    implementation(libs.arrow.core)
    implementation(libs.arrow.functions)
    implementation(libs.arrow.fx.coroutines)
    implementation(libs.arrow.resilience)
    implementation(libs.arrow.suspendapp)
    implementation(libs.arrow.suspendapp.ktor)
    implementation(libs.bundles.cxf)
    implementation(libs.bundles.logging)
    implementation(libs.bundles.prometheus)
    implementation(libs.ebxml.protokoll)
    implementation(libs.emottak.payload.xsd)
    implementation(libs.jaxb.runtime)
    implementation(libs.ktor.client.auth)
    implementation(libs.ktor.client.cio)
    implementation(libs.ktor.client.cio.jvm)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.ktor.server.auth.jvm)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.core.jvm)
    implementation(libs.ktor.server.call.logging.jvm)
    implementation(libs.ktor.server.netty)
    implementation(libs.labai.jsr305x.annotations)
    implementation(libs.postgresql)
    implementation(libs.sqldelight.jdbc.driver)
    implementation(libs.sqldelight.primitive.adapters)
    implementation(libs.token.validation.ktor.v3)
    runtimeOnly("net.java.dev.jna:jna:5.12.1")
    testImplementation("javax.activation:activation:1.1.1")
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
