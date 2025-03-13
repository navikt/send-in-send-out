package no.nav.emottak.ebms.plugin

import io.ktor.server.application.Application
import io.ktor.server.routing.routing
import io.micrometer.prometheus.PrometheusMeterRegistry
import no.nav.emottak.ebms.fagmeldingRoutes
import no.nav.emottak.ebms.healthcheckRoutes

fun Application.configureRoutes(prometheusMeterRegistry: PrometheusMeterRegistry) {
    routing {
        fagmeldingRoutes(prometheusMeterRegistry)
        healthcheckRoutes(prometheusMeterRegistry)
    }
}
