package no.nav.emottak.trekkopplysning

import com.ibm.mq.jms.MQQueueConnectionFactory
import com.ibm.msg.client.wmq.WMQConstants
import no.nav.emottak.config.TrekkopplysningMq
import no.nav.emottak.log
import no.nav.emottak.utils.environment.getEnvVar
import no.nav.emottak.utils.environment.getSecret
import javax.jms.Session

class JmsClient(
    config: TrekkopplysningMq,
    val factory: MQQueueConnectionFactory = MQQueueConnectionFactory(),
    val secretPath: String = getEnvVar("SERVICEUSERMQ_SECRET_PATH", "/dummy/path"),
    var username: String = getSecret("$secretPath/username", "testUsername"),
    var password: String = getSecret("$secretPath/password", "testPassword")
) {

    /*
    If we only supply queuemanager and no channel, it seems the connection will be made in "bind/server" mode.
    This led to the error message "Failed to load the IBM MQ native JNI library: 'mqjbnd'".
    We therefore must explicitly set the connection mode to client.

    There is no Channel defined in Fasit for old eMottak, only the queuemanager (MQLS04 in Q1).

    Also, to make the authentication work with username/pw longer than 12 characters, set up MQCSP authentication.
     */
    init {
        factory.setHostName(config.hostname.value)
        factory.setPort(config.port)
        factory.setQueueManager(config.queueManager)
        factory.setChannel(config.channel)
        factory.setIntProperty(WMQConstants.WMQ_CONNECTION_MODE, WMQConstants.WMQ_CM_CLIENT)
//        factory.setBooleanProperty(WMQConstants.USER_AUTHENTICATION_MQCSP, true)
        log.debug("MQ User: $username")
        // fallback mens jeg venter på at noen godkjenner vault-iac PR
        if (username.equals("testUsername")) {
            username = getEnvVar("MQ_USERNAME", "testUsername")
            password = getEnvVar("MQ_PASSWORD", "")
            log.debug("MQ User 2: $username")
        }
    }

    // Her opprettes ny connection (og lukkes) for hver melding.
    // Kan cache/poole connections hvis dette viser seg å bli for mye overhead
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
