package no.nav.emottak.ebms

import arrow.continuations.SuspendApp
import arrow.core.raise.result
import arrow.fx.coroutines.resourceScope
import io.ktor.server.application.Application
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.utils.io.CancellationException
import io.micrometer.prometheus.PrometheusConfig
import io.micrometer.prometheus.PrometheusMeterRegistry
import kotlinx.coroutines.awaitCancellation
import no.nav.emottak.ebms.plugin.configureAuthentication
import no.nav.emottak.ebms.plugin.configureContentNegotiation
import no.nav.emottak.ebms.plugin.configureCoroutineDebugger
import no.nav.emottak.ebms.plugin.configureMetrics
import no.nav.emottak.ebms.plugin.configureRoutes
import org.slf4j.LoggerFactory

internal val log = LoggerFactory.getLogger("no.nav.emottak.ebms.App")

fun main() = SuspendApp {
    result {
        resourceScope {
            val prometheusMeterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)

            val server = embeddedServer(Netty, port = 8080) {
                ebmsSendInModule(prometheusMeterRegistry)
            }.also { it.engineConfig.maxChunkSize = 100000 }

            server.start(wait = false)

            awaitCancellation()
        }
    }.onFailure { error ->
        when (error) {
            is CancellationException -> {}
            else -> logError(error)
        }
    }
}

internal fun Application.ebmsSendInModule(meterRegistry: PrometheusMeterRegistry) {
    configureMetrics(meterRegistry)
    configureContentNegotiation()
    configureAuthentication()
    configureCoroutineDebugger()
    configureRoutes(meterRegistry)
}

private fun logError(t: Throwable) = log.error("Shutdown ebms-send-in due to: ${t.stackTraceToString()}")
