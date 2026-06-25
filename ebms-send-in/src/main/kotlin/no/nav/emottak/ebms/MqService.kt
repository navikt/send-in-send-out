package no.nav.emottak.ebms

import no.nav.emottak.ebms.service.JmsClient
import no.trygdeetaten.xml.eiff._1.EIFellesformat

abstract class MqService(val jmSclient: JmsClient, val queue: String) {

    fun sendMessage(messageText: String) {
        jmSclient.sendMessage(queue, messageText)
    }

    fun verifyConnection() {
        jmSclient.verifyConnection()
    }

    abstract fun buildAndSend(fellesformat: EIFellesformat, payload: ByteArray)
}
