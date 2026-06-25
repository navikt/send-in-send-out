package no.nav.emottak.legemelding

import io.micrometer.core.instrument.MeterRegistry
import no.nav.emottak.config.MqQueueConfig
import no.nav.emottak.ebms.MqService
import no.nav.emottak.ebms.service.JmsClient
import no.nav.emottak.ebms.utils.recordMqMessage
import no.nav.emottak.fellesformat.FellesformatXmlBuilder
import no.nav.emottak.log
import no.trygdeetaten.xml.eiff._1.EIFellesformat

class LegeMeldingService(
    queueConfig: MqQueueConfig,
    private val meterRegistry: MeterRegistry? = null
) : MqService(
    jmSclient = JmsClient(queueConfig.mqConfig),
    queue = queueConfig.queue
) {

    override fun buildAndSend(fellesformat: EIFellesformat, payload: ByteArray) {
        val fellesformatXmlBuilder = FellesformatXmlBuilder()
        val messageBody = fellesformatXmlBuilder.buildXml(fellesformat.mottakenhetBlokk, payload)
        log.debug("Sending in legemelding with body: $messageBody")

        sendMessage(messageBody)
        meterRegistry?.recordMqMessage(
            queue = queue,
            mottakenhetBlokk = fellesformat.mottakenhetBlokk
        )
    }
}
