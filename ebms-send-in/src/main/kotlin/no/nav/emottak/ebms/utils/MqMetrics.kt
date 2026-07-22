package no.nav.emottak.ebms.utils

import io.micrometer.core.instrument.MeterRegistry
import no.trygdeetaten.xml.eiff._1.EIFellesformat

fun MeterRegistry.recordMqMessage(queue: String, mottakenhetBlokk: EIFellesformat.MottakenhetBlokk) =
    recordMqMessage(
        queue = queue,
        service = mottakenhetBlokk.ebService ?: "unknown",
        action = mottakenhetBlokk.ebAction ?: "unknown"
    )

fun MeterRegistry.recordMqMessage(queue: String, service: String, action: String) =
    counter(
        "mq_messages_total",
        "queue",
        queue,
        "service",
        service,
        "action",
        action
    ).increment()
