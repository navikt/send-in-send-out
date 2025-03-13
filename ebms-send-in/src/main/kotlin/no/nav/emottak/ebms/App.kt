package no.nav.emottak.ebms

import arrow.continuations.SuspendApp
import arrow.continuations.ktor.server
import arrow.core.raise.result
import arrow.fx.coroutines.ResourceScope
import arrow.fx.coroutines.resourceScope
import io.ktor.server.application.Application
import io.ktor.server.netty.Netty
import io.ktor.utils.io.CancellationException
import io.micrometer.prometheus.PrometheusConfig
import io.micrometer.prometheus.PrometheusMeterRegistry
import kotlinx.coroutines.awaitCancellation
import no.nav.emottak.config.config
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
            setupServer()
            awaitCancellation()
        }
    }.onFailure { error ->
        if (error !is CancellationException) logError(error)
    }
}

suspend fun ResourceScope.setupServer() {
    val server = config().server
    System.setProperty("io.netty.maxChunkSize", server.maxChunkSize)

    val prometheusMeterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)

    server(
        Netty,
        port = server.port,
        preWait = server.preWait,
        module = { ebmsSendInModule(prometheusMeterRegistry) }
    )
}

internal fun Application.ebmsSendInModule(prometheusMeterRegistry: PrometheusMeterRegistry) {
    configureMetrics(prometheusMeterRegistry)
    configureContentNegotiation()
    configureAuthentication()
    configureCoroutineDebugger()
    configureRoutes(prometheusMeterRegistry)
}

private fun logError(t: Throwable) = log.error("Shutdown ebms-send-in due to: ${t.stackTraceToString()}")
