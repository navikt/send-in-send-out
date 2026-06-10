package no.nav.emottak.legemelding

import io.micrometer.core.instrument.MeterRegistry
import no.nav.emottak.config.MqConfig
import no.nav.emottak.ebms.MqService
import no.nav.emottak.ebms.service.JmsClient
import no.nav.emottak.ebms.utils.recordMqMessage
import no.nav.emottak.fellesformat.insertPayload
import no.nav.emottak.log
import no.trygdeetaten.xml.eiff._1.EIFellesformat

class LegeMeldingService(
    paleMq: MqConfig,
    private val meterRegistry: MeterRegistry? = null
) : MqService(
    jmSclient = JmsClient(paleMq),
    queue = paleMq.queue
) {

    fun legemelding(fellesformat: EIFellesformat, payload: ByteArray) {
        var messageBody = marshalLegemelding(fellesformat)
        messageBody = insertPayload(messageBody, payload.toString(Charsets.UTF_8))
        log.debug("Sending in legemelding with body: $messageBody")

        sendMessage(messageBody)
        meterRegistry?.recordMqMessage(
            queue = queue,
            service = fellesformat.mottakenhetBlokk.ebService ?: "unknown",
            action = fellesformat.mottakenhetBlokk.ebAction ?: "unknown"
        )
    }
}
