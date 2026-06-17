package no.nav.emottak.sykmelding

import no.nav.emottak.config.MqConfig
import no.nav.emottak.ebms.service.JmsClient
import no.nav.emottak.fellesformat.FellesformatXmlBuilder
import no.nav.emottak.log
import no.trygdeetaten.xml.eiff._1.EIFellesformat

class SyfoMeldingService(syfoMq: MqConfig, val jmSclient: JmsClient = JmsClient(syfoMq), val queue: String = syfoMq.queue) {

    fun sendMessage(messageText: String) {
        jmSclient.sendMessage(queue, messageText)
    }

    fun verifyConnection() {
        jmSclient.verifyConnection()
    }

    fun sykmelding(fellesformat: EIFellesformat, payload: ByteArray) {
        val fellesformatXmlBuilder = FellesformatXmlBuilder()
        val messageBody = fellesformatXmlBuilder.buildXmlWithCustomMottakenhetBlokk(fellesformat.mottakenhetBlokk, payload)
        log.debug("Sending in sykmelding with body: " + messageBody)

        sendMessage(messageBody)
    }
}
