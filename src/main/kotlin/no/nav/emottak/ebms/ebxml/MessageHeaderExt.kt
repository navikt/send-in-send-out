/*
 * This Kotlin source file was generated by the Gradle 'init' task.
 */
package no.nav.emottak.ebms.ebxml

import org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.From
import org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.MessageData
import org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.MessageHeader
import org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.Service
import org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.To
import java.time.Instant
import java.util.*

fun MessageHeader.createResponseHeader(newAction: String?, newService: String?): MessageHeader {
    val messageHeader = MessageHeader()
    messageHeader.conversationId = this.conversationId
    messageHeader.from = From().also {
        it.partyId.addAll(this.to.partyId)
        it.role = this.to.role
    }
    messageHeader.to = To().also {
        it.partyId.addAll(this.from.partyId)
        it.role = this.from.role
    }
    messageHeader.service = if (newService != null) Service().also { it.value = newService } else this.service
    messageHeader.action = newAction ?: this.action
    messageHeader.cpaId = this.cpaId
    messageHeader.messageData = MessageData().also {
        it.messageId = this.messageData.messageId + "_RESPONSE"
        it.refToMessageId = this.messageData.messageId
        it.timestamp = Date.from(Instant.now())
    }
    return messageHeader
}
