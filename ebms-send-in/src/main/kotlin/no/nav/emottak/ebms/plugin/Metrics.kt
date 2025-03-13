package no.nav.emottak.ebms.plugin

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.micrometer.core.instrument.MeterRegistry

fun Application.configureMetrics(registry: MeterRegistry) {
    install(io.ktor.server.metrics.micrometer.MicrometerMetrics) {
        this.registry = registry
    }
}
