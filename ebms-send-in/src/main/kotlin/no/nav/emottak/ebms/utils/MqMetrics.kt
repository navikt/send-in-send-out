package no.nav.emottak.ebms.utils

import io.micrometer.core.instrument.MeterRegistry
import no.trygdeetaten.xml.eiff._1.EIFellesformat

fun MeterRegistry.recordMqMessage(queue: String, eiFellesformat: EIFellesformat) =
    recordMqMessage(
        queue = queue,
        service = eiFellesformat.mottakenhetBlokk?.ebService ?: "unknown",
        action = eiFellesformat.mottakenhetBlokk?.ebAction ?: "unknown"
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
