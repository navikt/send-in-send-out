package no.nav.emottak.ebms.plugin

import io.github.smiley4.ktoropenapi.OpenApi
import io.github.smiley4.ktoropenapi.config.OutputFormat
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.routing.routing
import io.micrometer.prometheus.PrometheusMeterRegistry
import no.nav.emottak.ebms.route.fagmeldingRoutes
import no.nav.emottak.ebms.route.healthcheckRoutes
import no.nav.emottak.ebms.route.openApiRoutes
import no.nav.emottak.util.EventRegistrationService

fun Application.configureRoutes(
    prometheusMeterRegistry: PrometheusMeterRegistry,
    eventRegistrationService: EventRegistrationService
) {
    install(OpenApi) {
        info {
            title = "eMottak Fagsystemintegrasjoner"
            version = "1.0.0"
            description = "Tjenester for å sende meldinger til fagsystem."
        }
        outputFormat = OutputFormat.JSON
    }

    routing {
        openApiRoutes()
        fagmeldingRoutes(prometheusMeterRegistry, eventRegistrationService)
        healthcheckRoutes(prometheusMeterRegistry)
    }
}
