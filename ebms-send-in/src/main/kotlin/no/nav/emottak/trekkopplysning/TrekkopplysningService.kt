package no.nav.emottak.trekkopplysning

import no.nav.emottak.config.TrekkopplysningMq
import no.nav.emottak.fellesformat.FellesFormatXmlMarshaller
import no.nav.emottak.log
import no.trygdeetaten.xml.eiff._1.EIFellesformat

class TrekkopplysningService(trekkopplysningMq: TrekkopplysningMq, val jmSclient: JmsClient = JmsClient(trekkopplysningMq), val queue: String = trekkopplysningMq.queue, val replyTo: String = trekkopplysningMq.replyTo) {

    fun sendMessage(messageText: String) {
        jmSclient.sendMessage(queue, messageText, replyTo)
    }

    fun verifyConnection() {
        jmSclient.verifyConnection()
    }

    fun trekkopplysning(fellesformat: EIFellesformat) {
        val messageBody = FellesFormatXmlMarshaller.marshal(fellesformat)
        log.debug(
            "Sending in trekkopplysning with body: " + messageBody
        )

        sendMessage(messageBody)
    }

    fun sendTestfile(versionId: Int) {
        val fileName = "version" + versionId + ".xml"
        val messageBody = this.javaClass.getResource("/mqtest/$fileName").readText()
//        log.debug(
//            "Sending in testfile with body: " + messageBody
//        )
        sendMessage(messageBody)
    }
}
