package no.nav.emottak.trekkopplysning

import io.micrometer.core.instrument.MeterRegistry
import no.nav.emottak.config.MqConfig
import no.nav.emottak.ebms.MqService
import no.nav.emottak.ebms.service.JmsClient
import no.nav.emottak.ebms.utils.recordMqMessage
import no.nav.emottak.log
import no.trygdeetaten.xml.eiff._1.EIFellesformat

class TrekkopplysningService(
    mqConfig: MqConfig,
    private val meterRegistry: MeterRegistry? = null
) : MqService(
    jmSclient = JmsClient(mqConfig),
    queue = mqConfig.queue
) {

    fun trekkopplysning(fellesformat: EIFellesformat) {
        val messageBody = marshalTrekkopplysning(fellesformat)
        log.debug("Sending in trekkopplysning with body: $messageBody")

        sendMessage(messageBody)
        meterRegistry?.recordMqMessage(
            queue = queue,
            service = fellesformat.mottakenhetBlokk.ebService ?: "unknown",
            action = fellesformat.mottakenhetBlokk.ebAction ?: "unknown"
        )
    }
}
