package no.nav.emottak.legemelding

import no.nav.emottak.config.MqConfig
import no.nav.emottak.ebms.service.JmsClient
import no.nav.emottak.fellesformat.FellesformatXmlBuilder
import no.nav.emottak.log
import no.trygdeetaten.xml.eiff._1.EIFellesformat

class LegeMeldingService(syfoMq: MqConfig, val jmSclient: JmsClient = JmsClient(syfoMq), val queue: String = syfoMq.queue) {

    fun sendMessage(messageText: String) {
        jmSclient.sendMessage(queue, messageText)
    }

    fun verifyConnection() {
        jmSclient.verifyConnection()
    }

    fun legemelding(fellesformat: EIFellesformat, payload: ByteArray) {
        val fellesformatXmlBuilder = FellesformatXmlBuilder()
//        val messageBody = fellesformatXmlBuilder.buildXmlWithCustomMottakenhetBlokk(fellesformat.mottakenhetBlokk, payload)
        val messageBody = fellesformatXmlBuilder.buildXml(fellesformat.mottakenhetBlokk, payload)
        log.debug("Sending in legemelding with body: " + messageBody)

        sendMessage(messageBody)
    }
}
