import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("java-library")
    val kotlinVersion = "2.4.10"
    kotlin("jvm") version kotlinVersion apply false
    kotlin("plugin.serialization") version kotlinVersion // apply false
    id("io.ktor.plugin") version "3.5.1" apply false
    id("org.jlleitschuh.gradle.ktlint") version "11.6.1"
    id("org.jlleitschuh.gradle.ktlint-idea") version "11.6.1"
}

tasks {
    ktlintFormat {
        this.enabled = true
    }

    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().all {
        compilerOptions {
            jvmTarget = JvmTarget.JVM_21
            optIn.add("kotlin.uuid.ExperimentalUuidApi")
        }
    }
}
