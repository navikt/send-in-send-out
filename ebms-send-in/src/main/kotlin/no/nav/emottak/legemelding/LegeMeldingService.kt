package no.nav.emottak.legemelding

import no.nav.emottak.config.MqConfig
import no.nav.emottak.ebms.service.JmsClient
import no.nav.emottak.log
import no.trygdeetaten.xml.eiff._1.EIFellesformat

class LegeMeldingService(syfoMq: MqConfig, val jmSclient: JmsClient = JmsClient(syfoMq), val queue: String = syfoMq.queue) {

    fun sendMessage(messageText: String) {
        jmSclient.sendMessage(queue, messageText)
    }

    fun verifyConnection() {
        jmSclient.verifyConnection()
    }

    fun legemelding(fellesformat: EIFellesformat) {
        val messageBody = marshalLegemelding(fellesformat)
        log.debug(
            "Sending in legemelding with body: " + messageBody
        )

        sendMessage(messageBody)
    }
}
