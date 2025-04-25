package no.nav.emottak.ebms.plugin

import io.ktor.server.application.Application
import io.ktor.server.routing.routing
import io.micrometer.prometheus.PrometheusMeterRegistry
import kotlinx.coroutines.CoroutineScope
import no.nav.emottak.ebms.route.fagmeldingRoutes
import no.nav.emottak.ebms.route.healthcheckRoutes
import no.nav.emottak.utils.kafka.service.EventLoggingService

fun Application.configureRoutes(
    prometheusMeterRegistry: PrometheusMeterRegistry,
    eventLoggingService: EventLoggingService,
    eventRegistrationScope: CoroutineScope
) {
    routing {
        fagmeldingRoutes(prometheusMeterRegistry, eventLoggingService, eventRegistrationScope)
        healthcheckRoutes(prometheusMeterRegistry)
    }
}
