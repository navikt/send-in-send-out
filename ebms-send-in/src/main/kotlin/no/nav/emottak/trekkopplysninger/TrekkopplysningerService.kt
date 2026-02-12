package no.nav.emottak.trekkopplysninger

import no.nav.emottak.config.TrekkopplysningerMq

class TrekkopplysningerService(trekkopplysningerMq: TrekkopplysningerMq, val jmSclient: JmsClient = JmsClient(trekkopplysningerMq), val queue: String = trekkopplysningerMq.queue) {

    fun sendMessage(messageText: String) {
        jmSclient.sendMessage(queue, messageText)
    }

    fun browse(): String {
        return jmSclient.browse(queue)
    }
    fun superBrowse(trekkopplysningerMq: TrekkopplysningerMq): String {
        return jmSclient.superBrowse(trekkopplysningerMq)
    }
}
