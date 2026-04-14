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
import no.nav.emottak.config.Configurator.config
import no.nav.emottak.ebms.kafka.EbmsOutPayloadProducer
import no.nav.emottak.ebms.kafka.launchEbmsInPayloadReceiver
import no.nav.emottak.ebms.kafka.launchEbmsOutFellesformatReceiver
import no.nav.emottak.ebms.plugin.configureAuthentication
import no.nav.emottak.ebms.plugin.configureContentNegotiation
import no.nav.emottak.ebms.plugin.configureCoroutineDebugger
import no.nav.emottak.ebms.plugin.configureMetrics
import no.nav.emottak.ebms.plugin.configureRoutes
import no.nav.emottak.trekkopplysning.TrekkopplysningService
import no.nav.emottak.util.EventRegistrationService
import no.nav.emottak.util.EventRegistrationServiceImpl
import no.nav.emottak.utils.coroutines.coroutineScope
import no.nav.emottak.utils.environment.getEnvVar
import no.nav.emottak.utils.kafka.client.EventPublisherClient
import no.nav.emottak.utils.kafka.service.EventLoggingService
import org.slf4j.LoggerFactory

// De fleste env-variablene hentes fra nais-yaml i dette prosjektet, så key i koden skal stemme med key i nais-yaml.
// For de følgende feature-flaggene hentes variabelverdien fra ekstern nais-config, med følgende keys.
// (Se https://console.nav.cloud.nais.io/team/team-emottak/dev-fss/config/ebms-send-in)
const val USE_ASYNC_IN_KEY = "USE_ASYNC_IN"
const val USE_ASYNC_OUT_KEY = "USE_ASYNC_OUT"

internal val log = LoggerFactory.getLogger("no.nav.emottak.App")

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

    val mqConfig = config().trekkopplysningMq
    val trekkopplysningService = TrekkopplysningService(mqConfig)
    log.info("Set up to use MQ with host ${mqConfig.hostname}, port ${mqConfig.port}, queueManager ${mqConfig.queueManager}, channel ${mqConfig.channel}, queue ${mqConfig.queue}")

    val outPayloadProducer = EbmsOutPayloadProducer(
        config().ebmsOutPayloadProducer.topic,
        config().kafka
    )

    val useAsyncIn = fixEnvStringFromConfig(getEnvVar(USE_ASYNC_IN_KEY, "false")).toBoolean()
    if (useAsyncIn) {
        log.info("Set up to read asynchronous inbound messages from EbmsInPayload topic")
        eventRegistrationScope.launchEbmsInPayloadReceiver(config(), eventRegistrationService, prometheusMeterRegistry, trekkopplysningService)
    } else {
        log.info("Asynchronous inbound messages turned OFF, will only receive synchronous calls")
    }
    val useAsyncOut = fixEnvStringFromConfig(getEnvVar(USE_ASYNC_OUT_KEY, "false")).toBoolean()
    if (useAsyncOut) {
        log.info("Set up to read asynchronous responses/outbound messages from Fellesformat topic")
        eventRegistrationScope.launchEbmsOutFellesformatReceiver(config(), eventRegistrationService, outPayloadProducer)
    } else {
        log.info("Asynchronous outbound messages turned OFF, will not process responses/outbound messages")
    }

    server(
        Netty,
        port = serverConfig.port.value,
        preWait = serverConfig.preWait,
        module = { ebmsSendInModule(prometheusMeterRegistry, eventRegistrationService, trekkopplysningService, useAsyncIn) }
    )
}

internal fun Application.ebmsSendInModule(
    prometheusMeterRegistry: PrometheusMeterRegistry,
    eventRegistrationService: EventRegistrationService,
    trekkopplysningService: TrekkopplysningService,
    useAsyncIn: Boolean
) {
    configureMetrics(prometheusMeterRegistry)
    configureContentNegotiation()
    configureAuthentication()
    configureCoroutineDebugger()
    configureRoutes(prometheusMeterRegistry, eventRegistrationService, trekkopplysningService, useAsyncIn)
}

// Boolske verdier i ekstern NAIS config må/bør være tekst-strenger, ellers kan de ikke redigeres
// De kommer da inn til applikasjonen med anførselstegnene i tekstverdien, som må fjernes for å kunne tolkes riktig.
internal fun fixEnvStringFromConfig(s: String): String {
    if (s != null && s.startsWith("\"") && s.endsWith("\"")) {
        return s.replace("\"", "")
    }
    return s
}
