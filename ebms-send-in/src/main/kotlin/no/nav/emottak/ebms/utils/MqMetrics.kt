package no.nav.emottak.ebms.utils

import io.micrometer.core.instrument.MeterRegistry

fun MeterRegistry.recordMqMessage(queue: String, service: String, action: String) {
    counter(
        "mq_messages_total",
        "queue",
        queue,
        "service",
        service,
        "action",
        action
    ).increment()
}
