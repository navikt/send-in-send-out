package no.nav.emottak.ebms.plugin

import io.ktor.server.application.Application
import io.ktor.server.routing.routing
import io.micrometer.prometheus.PrometheusMeterRegistry
import no.nav.emottak.ebms.route.fagmeldingRoutes
import no.nav.emottak.ebms.route.healthcheckRoutes
import no.nav.emottak.ebms.route.sendTestSykmelding
import no.nav.emottak.ebms.route.verifyMq
import no.nav.emottak.legemelding.LegeMeldingService
import no.nav.emottak.sykmelding.SyfoMeldingService
import no.nav.emottak.trekkopplysning.TrekkopplysningService
import no.nav.emottak.util.EventRegistrationService
import no.nav.emottak.utils.environment.isProdEnv

fun Application.configureRoutes(
    prometheusMeterRegistry: PrometheusMeterRegistry,
    eventRegistrationService: EventRegistrationService,
    trekkopplysningService: TrekkopplysningService,
    syfoMeldingService: SyfoMeldingService,
    legeMeldingService: LegeMeldingService,
    useAsyncIn: Boolean
) {
    routing {
        if (!isProdEnv()) {
            verifyMq(trekkopplysningService, syfoMeldingService, legeMeldingService)
            sendTestSykmelding(syfoMeldingService)
        }
        fagmeldingRoutes(prometheusMeterRegistry, eventRegistrationService, trekkopplysningService, syfoMeldingService, legeMeldingService, useAsyncIn)
        healthcheckRoutes(prometheusMeterRegistry)
    }
}
