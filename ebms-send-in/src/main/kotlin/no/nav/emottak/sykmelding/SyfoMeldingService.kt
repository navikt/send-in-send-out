package no.nav.emottak.sykmelding

import io.micrometer.core.instrument.MeterRegistry
import no.nav.emottak.config.MqConfig
import no.nav.emottak.ebms.MqService
import no.nav.emottak.ebms.service.JmsClient
import no.nav.emottak.ebms.utils.recordMqMessage
import no.nav.emottak.fellesformat.insertPayload
import no.nav.emottak.log
import no.trygdeetaten.xml.eiff._1.EIFellesformat

class SyfoMeldingService(
    syfoMq: MqConfig,
    private val meterRegistry: MeterRegistry? = null
) : MqService(
    jmSclient = JmsClient(syfoMq),
    queue = syfoMq.queue
) {

    fun sykmelding(fellesformat: EIFellesformat, payload: ByteArray) {
        var messageBody = marshalSykmelding(fellesformat)
        messageBody = insertPayload(messageBody, payload.toString(Charsets.UTF_8))
        log.debug("Sending in sykmelding with body: $messageBody")

        sendMessage(messageBody)
        meterRegistry?.recordMqMessage(
            queue = queue,
            service = fellesformat.mottakenhetBlokk.ebService ?: "unknown",
            action = fellesformat.mottakenhetBlokk.ebAction ?: "unknown"
        )
    }
}
