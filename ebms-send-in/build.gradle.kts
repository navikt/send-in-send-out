import com.github.jengelman.gradle.plugins.shadow.transformers.AppendingTransformer
import org.gradle.kotlin.dsl.testLibs

/*
 * This file was generated by the Gradle 'init' task.
 */

plugins {
    kotlin("jvm")
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
    implementation(libs.labai.jsr305x.annotations)
    implementation(libs.bundles.logging)
    implementation(libs.bundles.prometheus)
    implementation(libs.ebxml.protokoll)
    implementation(libs.emottak.payload.xsd)
    implementation(libs.jaxb.runtime)
    implementation(libs.token.validation.ktor.v2)
    implementation(libs.ktor.server.auth.jvm)
    implementation(libs.ktor.client.auth)
    implementation(libs.sqldelight.jdbc.driver)
    implementation(libs.sqldelight.primitive.adapters)
    implementation(libs.postgresql)
    implementation("io.ktor:ktor-client-cio-jvm:2.3.4")
    implementation("com.sun.xml.messaging.saaj:saaj-impl:1.5.1")
    implementation(libs.bundles.cxf)
    runtimeOnly("net.java.dev.jna:jna:5.12.1")
    testImplementation("javax.activation:activation:1.1.1")
    testImplementation(testLibs.ktor.server.test.host)
    testImplementation(testLibs.mock.oauth2.server)
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
