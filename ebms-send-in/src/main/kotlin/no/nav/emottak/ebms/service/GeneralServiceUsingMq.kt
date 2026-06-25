package no.nav.emottak.ebms.service

import io.micrometer.core.instrument.MeterRegistry
import no.nav.emottak.config.MqQueueConfig
import no.nav.emottak.ebms.MqService
import no.nav.emottak.ebms.utils.recordMqMessage
import no.nav.emottak.fellesformat.FellesformatXmlBuilder
import no.nav.emottak.log
import no.trygdeetaten.xml.eiff._1.EIFellesformat

// The general processing for services using MQ and FellesformatXmlBuilder: payload sent as it is, with a given MottakenhetBlokk
class GeneralServiceUsingMq(
    queueConfig: MqQueueConfig,
    private val meterRegistry: MeterRegistry? = null
) : MqService(
    jmSclient = JmsClient(queueConfig.mqConfig),
    queue = queueConfig.queue
) {

    override fun buildAndSend(fellesformat: EIFellesformat, payload: ByteArray) {
        val fellesformatXmlBuilder = FellesformatXmlBuilder()
        val messageBody = fellesformatXmlBuilder.buildXml(fellesformat.mottakenhetBlokk, payload)

        log.debug("Sending in message for ${fellesformat.mottakenhetBlokk.ebService} with body: " + messageBody)
        sendMessage(messageBody)
        meterRegistry?.recordMqMessage(
            queue = queue,
            mottakenhetBlokk = fellesformat.mottakenhetBlokk
        )
    }
}
