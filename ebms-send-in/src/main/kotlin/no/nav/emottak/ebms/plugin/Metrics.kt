package no.nav.emottak.ebms.plugin

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.metrics.micrometer.MicrometerMetrics
import io.micrometer.prometheus.PrometheusMeterRegistry

fun Application.configureMetrics(prometheusMeterRegistry: PrometheusMeterRegistry) {
    install(MicrometerMetrics) {
        this.registry = prometheusMeterRegistry
    }
}
