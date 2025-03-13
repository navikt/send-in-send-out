package no.nav.emottak.ebms.utils

import io.micrometer.core.instrument.Timer.ResourceSample
import io.micrometer.prometheus.PrometheusMeterRegistry

fun <T> timed(meterRegistry: PrometheusMeterRegistry, metricName: String, process: ResourceSample.() -> T): T =
    io.micrometer.core.instrument.Timer.resource(meterRegistry, metricName)
        .use {
            process(it)
        }
