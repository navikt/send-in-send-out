package no.nav.emottak.ebms

import no.nav.emottak.ebms.service.JmsClient

open class MqService(val jmSclient: JmsClient, val queue: String) {

    fun sendMessage(messageText: String) {
        jmSclient.sendMessage(queue, messageText)
    }

    fun verifyConnection() {
        jmSclient.verifyConnection()
    }
}
