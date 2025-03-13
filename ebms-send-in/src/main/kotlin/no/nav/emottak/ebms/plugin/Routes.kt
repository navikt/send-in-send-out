package no.nav.emottak.ebms.plugin

import io.ktor.server.application.Application
import io.ktor.server.routing.routing
import io.micrometer.prometheus.PrometheusMeterRegistry
import no.nav.emottak.ebms.fagmeldingRoutes
import no.nav.emottak.ebms.healthcheckRoutes

fun Application.configureRoutes(
    registry: PrometheusMeterRegistry
) {
    routing {
        fagmeldingRoutes(registry)
        healthcheckRoutes(registry)
    }
}
