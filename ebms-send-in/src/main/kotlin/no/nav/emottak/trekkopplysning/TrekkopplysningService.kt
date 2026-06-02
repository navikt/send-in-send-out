package no.nav.emottak.trekkopplysning

import no.nav.emottak.config.MqConfig
import no.nav.emottak.ebms.service.JmsClient
import no.nav.emottak.fellesformat.insertPayload
import no.nav.emottak.log
import no.trygdeetaten.xml.eiff._1.EIFellesformat

class TrekkopplysningService(mqConfig: MqConfig, val jmSclient: JmsClient = JmsClient(mqConfig), val queue: String = mqConfig.queue) {

    fun sendMessage(messageText: String) {
        jmSclient.sendMessage(queue, messageText)
    }

    fun verifyConnection() {
        jmSclient.verifyConnection()
    }

    fun trekkopplysning(fellesformat: EIFellesformat, payload: ByteArray) {
        var messageBody = marshalTrekkopplysning(fellesformat)
        messageBody = insertPayload(messageBody, payload.toString(Charsets.UTF_8))
        log.debug(
            "Sending in trekkopplysning with body: " + messageBody
        )

        sendMessage(messageBody)
    }
}
