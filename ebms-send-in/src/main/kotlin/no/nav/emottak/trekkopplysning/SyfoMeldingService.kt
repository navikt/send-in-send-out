package no.nav.emottak.trekkopplysning

import no.nav.emottak.config.MqConfig
import no.nav.emottak.log
import no.trygdeetaten.xml.eiff._1.EIFellesformat

class SyfoMeldingService(syfoMq: MqConfig, val jmSclient: JmsClient = JmsClient(syfoMq), val queue: String = syfoMq.queue) {

    fun sendMessage(messageText: String) {
        jmSclient.sendMessage(queue, messageText)
    }

    fun verifyConnection() {
        jmSclient.verifyConnection()
    }

    fun sykmelding(fellesformat: EIFellesformat) {
        val messageBody = marshalSykmelding(fellesformat)
        log.debug(
            "Sending in sykmelding with body: " + messageBody
        )

        sendMessage(messageBody)
    }
}
