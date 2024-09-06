package no.nav.emottak.utbetaling

import no.kith.xmlstds.msghead._2006_05_24.Document
import no.kith.xmlstds.msghead._2006_05_24.MsgHead
import no.kith.xmlstds.msghead._2006_05_24.RefDoc
import no.nav.emottak.melding.model.SendInRequest
import no.nav.emottak.util.toXMLGregorianCalendar
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.util.UUID

class MsgHeadUtil {

    fun msgHeadResponse(sendInRequest: SendInRequest, fagmeldingResponse: ByteArray): MsgHead {
        val msgHead: MsgHead =
            utbetalingXmlMarshaller
                .unmarshal(sendInRequest.payload.toString(Charsets.UTF_8), MsgHead::class.java)

        return msgHead.apply {
            msgInfo.apply {
                type.apply {
                    dn = "Svar på forespørsel om inntekt"
                    v = "InntektInformasjon"
                }
                genDate = Instant.now().toXMLGregorianCalendar()
                msgId = UUID.randomUUID().toString()
                ack.apply {
                    v = "N"
                    dn = "Nei"
                }
                val newReceiver = sender
                val newSender = receiver
                sender.apply { organisation = newSender.organisation }
                receiver.apply { organisation = newReceiver.organisation }
                conversationRef.apply {
                    refToParent = sendInRequest.messageId
                    refToConversation = sendInRequest.conversationId
                }
            }
            document.clear()
            document.add(
                Document().apply {
                    refDoc.apply {
                        msgType.apply {
                            v = "XML"
                            dn = "XML-instans"
                        }
                        mimeType = "text/xml"
                        content = RefDoc.Content().apply {
                            any.add(
                                listOf(String(fagmeldingResponse, StandardCharsets.UTF_8))
                            )
                        }
                    }
                }
            )
            signature = null // TODO?
        }
    }
}
