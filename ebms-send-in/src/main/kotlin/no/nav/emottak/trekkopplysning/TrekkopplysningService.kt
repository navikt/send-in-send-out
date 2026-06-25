package no.nav.emottak.trekkopplysning

import io.micrometer.core.instrument.MeterRegistry
import no.nav.emottak.config.MqQueueConfig
import no.nav.emottak.ebms.MqService
import no.nav.emottak.ebms.service.JmsClient
import no.nav.emottak.ebms.utils.recordMqMessage
import no.nav.emottak.fellesformat.FellesformatXmlBuilder
import no.nav.emottak.log
import no.trygdeetaten.xml.eiff._1.EIFellesformat

class TrekkopplysningService(
    queueConfig: MqQueueConfig,
    val useDomBuilder: Boolean,
    private val meterRegistry: MeterRegistry? = null
) : MqService(
    jmSclient = JmsClient(queueConfig.mqConfig),
    queue = queueConfig.queue
) {

    override fun buildAndSend(fellesformat: EIFellesformat, payload: ByteArray) {
        val messageBody = if (useDomBuilder) {
            val fellesformatXmlBuilder = FellesformatXmlBuilder()
            fellesformatXmlBuilder.buildXmlWithCustomMottakenhetBlokk(fellesformat.mottakenhetBlokk, payload)
        } else {
            marshalTrekkopplysning(fellesformat)
        }
        log.debug("Sending in trekkopplysning with body: " + messageBody)

        sendMessage(messageBody)
        meterRegistry?.recordMqMessage(
            queue = queue,
            mottakenhetBlokk = fellesformat.mottakenhetBlokk
        )
    }
}
