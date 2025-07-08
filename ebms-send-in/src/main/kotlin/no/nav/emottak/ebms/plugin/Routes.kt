package no.nav.emottak.ebms.plugin

import io.ktor.server.application.Application
import io.ktor.server.routing.routing
import io.micrometer.prometheus.PrometheusMeterRegistry
import no.nav.emottak.ebms.route.fagmeldingRoutes
import no.nav.emottak.ebms.route.healthcheckRoutes
import no.nav.emottak.util.EventRegistrationService

fun Application.configureRoutes(
    prometheusMeterRegistry: PrometheusMeterRegistry,
    eventRegistrationService: EventRegistrationService
) {
    routing {
        fagmeldingRoutes(prometheusMeterRegistry, eventRegistrationService)
        healthcheckRoutes(prometheusMeterRegistry)
    }
}
