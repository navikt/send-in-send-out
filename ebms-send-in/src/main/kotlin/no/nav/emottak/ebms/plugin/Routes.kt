package no.nav.emottak.ebms.plugin

import io.ktor.server.application.Application
import io.ktor.server.routing.routing
import io.micrometer.prometheus.PrometheusMeterRegistry
import no.nav.emottak.ebms.route.browseMqRoute
import no.nav.emottak.ebms.route.fagmeldingRoutes
import no.nav.emottak.ebms.route.healthcheckRoutes
import no.nav.emottak.trekkopplysninger.TrekkopplysningerService
import no.nav.emottak.util.EventRegistrationService
import no.nav.emottak.utils.environment.isProdEnv

fun Application.configureRoutes(
    prometheusMeterRegistry: PrometheusMeterRegistry,
    eventRegistrationService: EventRegistrationService,
    trekkopplysningerService: TrekkopplysningerService
) {
    routing {
        if (!isProdEnv()) {
            browseMqRoute(trekkopplysningerService)
        }
        fagmeldingRoutes(prometheusMeterRegistry, eventRegistrationService)
        healthcheckRoutes(prometheusMeterRegistry)
    }
}
