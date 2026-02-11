package no.nav.emottak.trekkopplysninger

import com.ibm.mq.jms.MQQueueConnectionFactory
import no.nav.emottak.config.TrekkopplysningerMq
import javax.jms.Session

class JmsClient(config: TrekkopplysningerMq, val factory: MQQueueConnectionFactory = MQQueueConnectionFactory()) {

    init {
        factory.setHostName(config.hostname.value)
        factory.setPort(config.port)
        factory.setQueueManager(config.queueManager)
        //        factory.setChannel("SYSTEM.DEF.SVRCONN") // channel brukes visst ikke av gamle emottak ?
    }

    fun sendMessage(queue: String, messageText: String) {
        factory.createContext(Session.AUTO_ACKNOWLEDGE)?.use {
            val queue = it.createQueue(queue)
            it.createProducer().send(queue, messageText)
        }
    }

    fun browse(queue: String): String {
        val sb: StringBuilder = StringBuilder()
        sb.append("MQ Browse report\n")
        factory.createContext(Session.AUTO_ACKNOWLEDGE)?.use {
            it.createBrowser(it.createQueue(queue)).use { browser ->
                {
                    var c = 0
                    while (browser.enumeration.hasMoreElements()) {
                        sb.append(browser.enumeration.nextElement())
                        sb.append("\n")
                        c++
                    }
                    sb.append("Fant $c meldinger\n")
                }
            }
        }
        sb.append("End of MQ Browse report")
        return sb.toString()
    }
}
