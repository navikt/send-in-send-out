package no.nav.emottak.ebms.utils

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer.ResourceSample
import io.micrometer.core.instrument.Timer.resource

fun <T> timed(meterRegistry: MeterRegistry, metricName: String, process: ResourceSample.() -> T): T =
    resource(meterRegistry, metricName).use { process(it) }
