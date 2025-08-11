package no.nav.emottak

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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitCancellation
import no.nav.emottak.config.config
import no.nav.emottak.ebms.plugin.configureAuthentication
import no.nav.emottak.ebms.plugin.configureContentNegotiation
import no.nav.emottak.ebms.plugin.configureCoroutineDebugger
import no.nav.emottak.ebms.plugin.configureMetrics
import no.nav.emottak.ebms.plugin.configureRoutes
import no.nav.emottak.util.EventRegistrationService
import no.nav.emottak.util.EventRegistrationServiceImpl
import no.nav.emottak.utils.coroutines.coroutineScope
import no.nav.emottak.utils.kafka.client.EventPublisherClient
import no.nav.emottak.utils.kafka.service.EventLoggingService
import org.slf4j.LoggerFactory

internal val log = LoggerFactory.getLogger("no.nav.emottak.ebms.App")

fun main() = SuspendApp {
    result {
        resourceScope {
            setupServer()
            awaitCancellation()
        }
    }.onFailure { error ->
        if (error !is CancellationException) {
            log.error("Shutdown ebms-send-in due to: ${error.stackTraceToString()}")
        }
    }
}

suspend fun ResourceScope.setupServer() {
    val serverConfig = config().server

    val prometheusMeterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)

    val kafkaPublisherClient = EventPublisherClient(config().kafka)
    val eventLoggingService = EventLoggingService(config().eventLogging, kafkaPublisherClient)

    val eventRegistrationScope = coroutineScope(Dispatchers.IO)

    val eventRegistrationService = EventRegistrationServiceImpl(eventLoggingService, eventRegistrationScope)

    server(
        Netty,
        port = serverConfig.port.value,
        preWait = serverConfig.preWait,
        module = { ebmsSendInModule(prometheusMeterRegistry, eventRegistrationService) }
    )
}

internal fun Application.ebmsSendInModule(
    prometheusMeterRegistry: PrometheusMeterRegistry,
    eventRegistrationService: EventRegistrationService
) {
    configureMetrics(prometheusMeterRegistry)
    configureContentNegotiation()
    configureAuthentication()
    configureCoroutineDebugger()
    configureRoutes(prometheusMeterRegistry, eventRegistrationService)
}
