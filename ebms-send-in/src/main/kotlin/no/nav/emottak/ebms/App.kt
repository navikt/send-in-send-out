package no.nav.emottak.ebms

import io.ktor.server.application.Application
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.micrometer.prometheus.PrometheusConfig
import io.micrometer.prometheus.PrometheusMeterRegistry
import no.nav.emottak.ebms.plugin.configureAuthentication
import no.nav.emottak.ebms.plugin.configureContentNegotiation
import no.nav.emottak.ebms.plugin.configureCoroutineDebugger
import no.nav.emottak.ebms.plugin.configureMetrics
import no.nav.emottak.ebms.plugin.configureRoutes
import org.slf4j.LoggerFactory

internal val log = LoggerFactory.getLogger("no.nav.emottak.ebms.App")

fun main() {
    log.info("ebms-send-in starting")

    System.setProperty("io.ktor.http.content.multipart.skipTempFile", "true")
    embeddedServer(Netty, port = 8080, module = { ebmsSendInModule() })
        .also { it.engineConfig.maxChunkSize = 100000 }
        .start(wait = true)
}

internal fun Application.ebmsSendInModule() {
    val appMicrometerRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
    configureMetrics(appMicrometerRegistry)
    configureContentNegotiation()
    configureAuthentication()
    configureCoroutineDebugger()
    configureRoutes(appMicrometerRegistry)
}
