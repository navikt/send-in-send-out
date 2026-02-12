package no.nav.emottak.trekkopplysninger

import com.ibm.mq.jms.MQQueueConnectionFactory
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
    We must use an eMottak channel, e.g. Q1_EMOTTAK_ASYNCH

    Also, to make the authentication work with username/pw longer than 12 characters, set up MQCSP authentication.
     */
    init {
        factory.setHostName(config.hostname.value)
        factory.setPort(config.port)
        factory.setQueueManager(config.queueManager)
        factory.setChannel(config.channel)
        // mulig vi ikke trenger disse to når vi angir channel, sjekk ut ???
//        factory.setIntProperty(WMQConstants.WMQ_CONNECTION_MODE, WMQConstants.WMQ_CM_CLIENT)
//        factory.setBooleanProperty(WMQConstants.USER_AUTHENTICATION_MQCSP, true)
    }

    fun sendMessage(queue: String, messageText: String) {
        factory.createContext(username, password, Session.AUTO_ACKNOWLEDGE)?.use {
            val queue = it.createQueue(queue)
            it.createProducer().send(queue, messageText)
        }
    }

    fun verifyConnection() {
        factory.createContext(username, password, Session.AUTO_ACKNOWLEDGE)
    }
}
