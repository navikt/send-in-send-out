package no.nav.emottak.legemelding

import io.micrometer.core.instrument.MeterRegistry
import no.nav.emottak.config.MqConfig
import no.nav.emottak.ebms.MqService
import no.nav.emottak.ebms.service.JmsClient
import no.nav.emottak.ebms.utils.recordMqMessage
import no.nav.emottak.fellesformat.FellesformatXmlBuilder
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
        val fellesformatXmlBuilder = FellesformatXmlBuilder()
        val messageBody = fellesformatXmlBuilder.buildXml(fellesformat.mottakenhetBlokk, payload)
        log.debug("Sending in legemelding with body: $messageBody")

        sendMessage(messageBody)
        meterRegistry?.recordMqMessage(
            queue = queue,
            eiFellesformat = fellesformat
        )
    }
}
