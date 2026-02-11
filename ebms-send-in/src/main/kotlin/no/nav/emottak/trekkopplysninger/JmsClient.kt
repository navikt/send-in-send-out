package no.nav.emottak.trekkopplysninger

import com.ibm.mq.jms.MQQueueConnectionFactory
import com.ibm.msg.client.wmq.WMQConstants
import no.nav.emottak.config.TrekkopplysningerMq
import javax.jms.Session

class JmsClient(
    config: TrekkopplysningerMq,
    val factory: MQQueueConnectionFactory = MQQueueConnectionFactory(),
    val username: String = config.username,
    val password: String = config.password
) {

    /*
    If we only supply queuemanager and no channel, it seems the connection will be made in "bind/server" mode.
    This led to the error message "Failed to load the IBM MQ native JNI library: 'mqjbnd'".
    There is no Channel defined in Fasit for old eMottak, only the queuemanager (MQLS04 in Q1).
    We should maybe have a Channel set up to connect to mq://b27apvl222.preprod.local:1413/MQLS04.
    The other alternative, which may work with no channel, is to explicitly set the connection mode to client.

    Also, to make the authentication work with username/pw longer than 12 characters, set up MQCSP authentication.
     */
    init {
        factory.setHostName(config.hostname.value)
        factory.setPort(config.port)
        factory.setQueueManager(config.queueManager)
        factory.setIntProperty(WMQConstants.WMQ_CONNECTION_MODE, WMQConstants.WMQ_CM_CLIENT)
        factory.setBooleanProperty(WMQConstants.USER_AUTHENTICATION_MQCSP, true)
        factory.setBooleanProperty(WMQConstants.USER_AUTHENTICATION_MQCSP, true)
        // factory.setChannel("SYSTEM.DEF.SVRCONN") // channel brukes visst ikke av gamle emottak ?
    }

    fun sendMessage(queue: String, messageText: String) {
        factory.createContext(username, password, Session.AUTO_ACKNOWLEDGE)?.use {
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
