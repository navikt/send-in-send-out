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
import no.nav.emottak.ebms.MqServiceMapper
import no.nav.emottak.ebms.kafka.EbmsOutPayloadProducer
import no.nav.emottak.ebms.kafka.launchEbmsInPayloadReceiver
import no.nav.emottak.ebms.kafka.launchEbmsOutFellesformatReceiver
import no.nav.emottak.ebms.plugin.configureAuthentication
import no.nav.emottak.ebms.plugin.configureContentNegotiation
import no.nav.emottak.ebms.plugin.configureMetrics
import no.nav.emottak.ebms.plugin.configureRoutes
import no.nav.emottak.ebms.service.GeneralServiceUsingMq
import no.nav.emottak.ebms.utils.AsyncRoutingAction
import no.nav.emottak.ebms.utils.SupportedAsyncServiceType
import no.nav.emottak.legemelding.LegeMeldingService
import no.nav.emottak.sykmelding.SyfoMeldingService
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
const val USE_DOMBUILDER_TREKKOPPLYSNING_KEY = "USE_DOMBUILDER"

// todo så lenge vi sender noen Legemeldinger til nye og noen til gamle eMottak,
//  bruker vi et id-prefiks for å skille ut de som hører til nye eMottak.
// Når dette skal fjernes:
// 1) endre asynch-router så ALLE legemeldinger går til nye emottak, uavhengig av prefiks
// 2) fjern prefikset under og bruken av det
const val NYE_EMOTTAK_LEGEMELDING_ID_PREFIX = "nye-emottak-"

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

    val trekkOpplysningMq = config().trekkOpplysningMq
    val trekkopplysningService = TrekkopplysningService(
        trekkOpplysningMq,
        getEnvVar(USE_DOMBUILDER_TREKKOPPLYSNING_KEY, "false").fixEnvStringFromConfig().toBoolean().also {
            log.info("Set up Trekkopplysning to use DOM builder to generate XML: $it")
        },
        meterRegistry = prometheusMeterRegistry
    )
    log.info("Set up Trekkopplysning to use MQ with config ${trekkOpplysningMq.mqConfig}, queue ${trekkOpplysningMq.queue}")
    val syfoMq = config().syfoMq
    val syfoMeldingService = SyfoMeldingService(syfoMq, meterRegistry = prometheusMeterRegistry)
    log.info("Set up Sykemeldinger to use MQ with config ${syfoMq.mqConfig}, queue ${syfoMq.queue}")
    val paleMq = config().paleMq
    val legeMeldingService = LegeMeldingService(paleMq, meterRegistry = prometheusMeterRegistry)
    log.info("Set up Legemeldinger to use MQ with config ${paleMq.mqConfig}, queue ${paleMq.queue}")
    val oppgjorMq = config().oppgjorMq
    val behandlerKravService = GeneralServiceUsingMq(oppgjorMq, meterRegistry = prometheusMeterRegistry)
    log.info("Set up Behandlerkrav to use MQ with config ${oppgjorMq.mqConfig}, queue ${oppgjorMq.queue}")
    val oppgjorsKontrollSvarMq = config().ereseptApprecMq
    val oppgjorsKontrollSvarService = GeneralServiceUsingMq(oppgjorsKontrollSvarMq, meterRegistry = prometheusMeterRegistry)
    log.info("Set up Oppgjørskontroll,Svarmelding to use MQ with config ${oppgjorsKontrollSvarMq.mqConfig}, queue ${oppgjorsKontrollSvarMq.queue}")
    val oppgjorsKontrollKravMq = config().ereseptM18Mq
    val oppgjorsKontrollKravService = GeneralServiceUsingMq(oppgjorsKontrollKravMq, meterRegistry = prometheusMeterRegistry)
    log.info("Set up Oppgjørskontroll,Oppgjørskrav to use MQ with config ${oppgjorsKontrollKravMq.mqConfig}, queue ${oppgjorsKontrollKravMq.queue}")
    val eiaMq = config().eiaMq
    val dialogMoteResponsService = GeneralServiceUsingMq(eiaMq, meterRegistry = prometheusMeterRegistry)
    log.info("Set up Dialogmøteinnkalling, Møterespons to use MQ with config ${eiaMq.mqConfig}, queue ${eiaMq.queue}")
    val dialogMq = config().dialogMq
    val dialogMoteKvitteringService = GeneralServiceUsingMq(dialogMq, meterRegistry = prometheusMeterRegistry)
    log.info("Set up Dialogmøteinnkalling, Kvittering to use MQ with config ${dialogMq.mqConfig}, queue ${dialogMq.queue}")
    val foresporselFraSaksbehandlerSvarService = GeneralServiceUsingMq(eiaMq, meterRegistry = prometheusMeterRegistry)
    log.info("Set up ForespørselFraSaksbehandler, Forespørselsvar to use MQ with config ${eiaMq.mqConfig}, queue ${eiaMq.queue}")
    val foresporselFraSaksbehandlerKvitteringService = GeneralServiceUsingMq(dialogMq, meterRegistry = prometheusMeterRegistry)
    log.info("Set up ForespørselFraSaksbehandler, Kvittering to use MQ with config ${dialogMq.mqConfig}, queue ${dialogMq.queue}")
    val henvendelseFraLegeService = GeneralServiceUsingMq(eiaMq, meterRegistry = prometheusMeterRegistry)
    log.info("Set up HenvendelseFraLege to use MQ with config ${eiaMq.mqConfig}, queue ${eiaMq.queue}")
    val henvendelseFraSaksbehandlerService = GeneralServiceUsingMq(dialogMq, meterRegistry = prometheusMeterRegistry)
    log.info("Set up HenvendelseFraSaksbehandler to use MQ with config ${dialogMq.mqConfig}, queue ${dialogMq.queue}")
    val oppfolgingsPlanService = GeneralServiceUsingMq(dialogMq, meterRegistry = prometheusMeterRegistry)
    log.info("Set up Oppfølgingsplan to use MQ with config ${dialogMq.mqConfig}, queue ${dialogMq.queue}")

    val mqServiceMapper = MqServiceMapper()
    mqServiceMapper.addMqService(SupportedAsyncServiceType.Trekkopplysning, mqService = trekkopplysningService)
    mqServiceMapper.addMqService(SupportedAsyncServiceType.Sykmelding, mqService = syfoMeldingService)
    mqServiceMapper.addMqService(SupportedAsyncServiceType.Legemelding, mqService = legeMeldingService)
    mqServiceMapper.addMqService(SupportedAsyncServiceType.BehandlerKrav, mqService = behandlerKravService)
    mqServiceMapper.addMqService(AsyncRoutingAction.OppgjorsKontrollSvarmelding, mqService = oppgjorsKontrollSvarService)
    mqServiceMapper.addMqService(AsyncRoutingAction.OppgjorsKontrollOppgjorskrav, mqService = oppgjorsKontrollKravService)
    mqServiceMapper.addMqService(AsyncRoutingAction.DialogmoteInnkallingMoteRespons, mqService = dialogMoteResponsService)
    mqServiceMapper.addMqService(AsyncRoutingAction.DialogmoteInnkallingKvittering, mqService = dialogMoteKvitteringService)
    mqServiceMapper.addMqService(AsyncRoutingAction.ForesporselFraSaksbehandlerForesporselSvar, mqService = foresporselFraSaksbehandlerSvarService)
    mqServiceMapper.addMqService(AsyncRoutingAction.ForesporselFraSaksbehandlerKvittering, mqService = foresporselFraSaksbehandlerKvitteringService)
    mqServiceMapper.addMqService(SupportedAsyncServiceType.HenvendelseFraLege, mqService = henvendelseFraLegeService)
    mqServiceMapper.addMqService(SupportedAsyncServiceType.HenvendelseFraSaksbehandler, mqService = henvendelseFraSaksbehandlerService)
    mqServiceMapper.addMqService(SupportedAsyncServiceType.Oppfolgingsplan, mqService = oppfolgingsPlanService)

    val outPayloadProducer = EbmsOutPayloadProducer(
        config().ebmsOutPayloadProducer.topic,
        config().kafka
    )

    val useAsyncIn = getEnvVar(USE_ASYNC_IN_KEY, "false").fixEnvStringFromConfig().toBoolean()
    if (useAsyncIn) {
        log.info("Set up to read asynchronous inbound messages from EbmsInPayload topic")
        eventRegistrationScope.launchEbmsInPayloadReceiver(config(), eventRegistrationService, prometheusMeterRegistry, mqServiceMapper)
    } else {
        log.info("Asynchronous inbound messages turned OFF, will only receive synchronous calls")
    }
    val useAsyncOut = getEnvVar(USE_ASYNC_OUT_KEY, "false").fixEnvStringFromConfig().toBoolean()
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
        module = { ebmsSendInModule(prometheusMeterRegistry, eventRegistrationService, mqServiceMapper, useAsyncIn) }
    )
}

internal fun Application.ebmsSendInModule(
    prometheusMeterRegistry: PrometheusMeterRegistry,
    eventRegistrationService: EventRegistrationService,
    mqServiceMapper: MqServiceMapper,
    useAsyncIn: Boolean
) {
    configureMetrics(prometheusMeterRegistry)
    configureContentNegotiation()
    configureAuthentication()
    configureRoutes(prometheusMeterRegistry, eventRegistrationService, mqServiceMapper, useAsyncIn)
}

// Boolske verdier i ekstern NAIS config må/bør være tekst-strenger, ellers kan de ikke redigeres
// De kommer da inn til applikasjonen med anførselstegnene i tekstverdien, som må fjernes for å kunne tolkes riktig.
internal fun String.fixEnvStringFromConfig() = removeSurrounding("\"")
