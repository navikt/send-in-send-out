/*
 * This file was generated by the Gradle 'init' task.
 *
 * The settings file is used to specify which projects to include in your build.
 *
 * Detailed information about configuring a multi-project build in Gradle can be found
 * in the user manual at https://docs.gradle.org/8.1.1/userguide/multi_project_builds.html
 */

dependencyResolutionManagement {

    versionCatalogs {
        create("libs") {
            version("bouncycastle", "1.76")
            version("exposed", "0.47.0")
            version("ktor", "2.3.8")
            version("jakarta-mail", "2.1.2")
            version("eclipse-angus", "2.0.2")
            version("jsch", "0.2.16")
            version("cxf", "3.5.5")

            library("bcpkix-jdk18on", "org.bouncycastle", "bcpkix-jdk18on").versionRef("bouncycastle")
            library("bcprov-jdk18on", "org.bouncycastle", "bcprov-jdk18on").versionRef("bouncycastle")
//            library("bcpkix-jdk18on", "org.bouncycastle", "bcpkix-debug-jdk18on").versionRef("bouncycastle")
//            library("bcprov-jdk18on", "org.bouncycastle", "bcprov-debug-jdk18on").versionRef("bouncycastle")
            library("apache-santuario", "org.apache.santuario:xmlsec:3.0.2")

            library("exposed-core", "org.jetbrains.exposed", "exposed-core").versionRef("exposed")
            library("exposed-dao", "org.jetbrains.exposed", "exposed-dao").versionRef("exposed")
            library("exposed-java-time", "org.jetbrains.exposed", "exposed-java-time").versionRef("exposed")
            library("exposed-jdbc", "org.jetbrains.exposed", "exposed-jdbc").versionRef("exposed")
            library("exposed-json", "org.jetbrains.exposed", "exposed-json").versionRef("exposed")

            library("jsch", "com.github.mwiede", "jsch").versionRef("jsch")

            library("ktor-server-core", "io.ktor", "ktor-server-core").versionRef("ktor")
            library("ktor-server-netty", "io.ktor", "ktor-server-netty").versionRef("ktor")
            library("ktor-server-call-logging-jvm", "io.ktor", "ktor-server-call-logging-jvm").versionRef("ktor")
            library("ktor-server-content-negotiation", "io.ktor", "ktor-server-content-negotiation").versionRef("ktor")
            library("ktor-client-content-negotiation", "io.ktor", "ktor-client-content-negotiation").versionRef("ktor")
            library("ktor-serialization-kotlinx-json", "io.ktor", "ktor-serialization-kotlinx-json").versionRef("ktor")
            library("ktor-client-core", "io.ktor", "ktor-client-core").versionRef("ktor")
            library("ktor-client-cio", "io.ktor", "ktor-client-cio").versionRef("ktor")
            library("ktor-client-auth", "io.ktor", "ktor-client-auth").versionRef("ktor")

            library("ktor-server-metrics-micrometer", "io.ktor", "ktor-server-metrics-micrometer").versionRef("ktor")
            library("micrometer-registry-prometheus", "io.micrometer:micrometer-registry-prometheus:1.12.4")

            library("logback-classic", "ch.qos.logback:logback-classic:1.4.11")
            library("logback-logstash", "net.logstash.logback:logstash-logback-encoder:7.4")

            library("hikari", "com.zaxxer:HikariCP:5.0.1")
            library("labai-jsr305x-annotations", "com.github.labai:labai-jsr305x-annotations:0.0.2")
            library("jakarta.xml.bind-api", "jakarta.xml.bind:jakarta.xml.bind-api:4.0.0")
            library("ebxml-protokoll", "no.nav.emottak:ebxml-protokoll:0.0.6")
            library("ktor-server-auth-jvm", "io.ktor:ktor-server-auth-jvm:2.3.8")
            library("token-validation-ktor-v2", "no.nav.security:token-validation-ktor-v2:4.1.4")
            library("flyway-core", "org.flywaydb:flyway-core:9.16.3")
            library("emottak-payload-xsd", "no.nav.emottak:emottak-payload-xsd:0.0.6")
            library("jaxb-runtime", "org.glassfish.jaxb:jaxb-runtime:2.4.0-b180830.0438")
            library("cxf-rt-frontend-jaxws", "org.apache.cxf", "cxf-rt-frontend-jaxws").versionRef("cxf")
            library("cxf-rt-transports-http", "org.apache.cxf", "cxf-rt-transports-http").versionRef("cxf")
            library("cxf-rt-ws-security", "org.apache.cxf", "cxf-rt-ws-security").versionRef("cxf")
            library("cxf-rt-features-logging", "org.apache.cxf", "cxf-rt-features-logging").versionRef("cxf")
            library("jakarta.xml.ws-api", "jakarta.xml.ws:jakarta.xml.ws-api:2.3.3")
            library("ojdbc8", "com.oracle.ojdbc:ojdbc8:19.3.0.0")

            library("jakarta-mail-api", "jakarta.mail", "jakarta.mail-api").versionRef("jakarta-mail")
            library("eclipse-angus", "org.eclipse.angus", "jakarta.mail").versionRef("eclipse-angus")

            bundle("prometheus", listOf("ktor-server-metrics-micrometer", "micrometer-registry-prometheus"))
            bundle("cxf", listOf("cxf-rt-frontend-jaxws", "cxf-rt-transports-http", "cxf-rt-ws-security", "cxf-rt-features-logging", "jakarta.xml.ws-api"))
            bundle("jakarta-mail", listOf("jakarta-mail-api", "eclipse-angus"))
            bundle("bouncycastle", listOf("bcpkix-jdk18on", "bcprov-jdk18on"))
            bundle("exposed", listOf("exposed-core", "exposed-dao", "exposed-java-time", "exposed-jdbc", "exposed-json"))
            bundle("logging", listOf("logback-classic", "logback-logstash"))
        }

        create("testLibs") {
            version("junit", "5.9.2")
            version("hamcrest", "2.2")
            version("mockk", "1.13.10")
            version("testPostgres", "1.18.0")
            version("xmlunit", "2.9.1")
            version("ktor-server-test", "2.3.4")
            version("kotest", "5.8.0")

            library("ktor-server-test-host", "io.ktor", "ktor-server-test-host").versionRef("ktor-server-test")
            library("junit-jupiter-api", "org.junit.jupiter", "junit-jupiter-api").versionRef("junit")
            library("junit-jupiter-engine", "org.junit.jupiter", "junit-jupiter-engine").versionRef("junit")
            library("junit-jupiter-params", "org.junit.jupiter", "junit-jupiter-params").versionRef("junit")
            library("mock-oauth2-server", "no.nav.security:mock-oauth2-server:2.1.2")

            library("hamcrest", "org.hamcrest", "hamcrest").versionRef("hamcrest")

            library("mockk-jvm", "io.mockk", "mockk-jvm").versionRef("mockk")
            library("mockk-dsl-jvm", "io.mockk", "mockk-dsl-jvm").versionRef("mockk")

            library("postgresql", "org.testcontainers", "postgresql").versionRef("testPostgres")

            library("xmlunit-core", "org.xmlunit", "xmlunit-core").versionRef("xmlunit")
            library("xmlunit-matchers", "org.xmlunit", "xmlunit-matchers").versionRef("xmlunit")

            library("kotest-runner-junit5", "io.kotest", "kotest-runner-junit5").versionRef("kotest")
            library("kotest-framework-datatest", "io.kotest", "kotest-framework-datatest").versionRef("kotest")

            bundle("kotest", listOf("kotest-runner-junit5", "kotest-framework-datatest"))
            bundle("mockk", listOf("mockk-jvm", "mockk-dsl-jvm"))
            bundle("xmlunit", listOf("xmlunit-core", "xmlunit-matchers"))
        }
    }

    repositories {
        mavenCentral()
        maven {
            name = "Ebxml protokoll"
            url = uri("https://maven.pkg.github.com/navikt/ebxml-protokoll")
            credentials {
                username = "token"
                password = System.getenv("GITHUB_TOKEN")
            }
        }
        maven {
            name = "Emottak payload format"
            url = uri("https://maven.pkg.github.com/navikt/emottak-payload-xsd")
            credentials {
                username = "token"
                password = System.getenv("GITHUB_TOKEN")
            }
        }
        maven {
            name = "Emottak ebms model"
            url = uri("https://maven.pkg.github.com/navikt/ebxml-processor")
            credentials {
                username = "token"
                password = System.getenv("GITHUB_TOKEN")
            }
        }
        maven {
            name = "Nav token-support"
            url = uri("https://maven.pkg.github.com/navikt/token-support")
            credentials {
                username = "token"
                password = System.getenv("GITHUB_TOKEN")
            }
        }
        maven {
            name = "Mock OAUTH2 server"
            url = uri("https://maven.pkg.github.com/navikt/mock-oauth2-server")
            credentials {
                username = "token"
                password = System.getenv("GITHUB_TOKEN")
            }
        }
    }
}

rootProject.name = "send-in-send-out"
include("felles", "smtp-transport", "ebms-send-in", "cpa-sync", "ebms-http")
