/**
 * Copyright 2011 Clockwork
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package no.nav.emottak.ebms.model

import no.nav.emottak.constants.EbXMLConstants.EBMS_SERVICE_URI
import no.nav.emottak.constants.EbXMLConstants.MESSAGE_ERROR_ACTION
import no.nav.emottak.constants.EbXMLConstants.OASIS_EBXML_MSG_HEADER_TAG
import no.nav.emottak.constants.EbXMLConstants.OASIS_EBXML_MSG_HEADER_XSD_NS_URI
import no.nav.emottak.ebms.Acknowledgment
import no.nav.emottak.ebms.EbmsFail
import no.nav.emottak.ebms.EbmsMessage
import no.nav.emottak.ebms.PayloadMessage
import no.nav.emottak.ebms.ebxml.ackRequested
import no.nav.emottak.ebms.ebxml.acknowledgment
import no.nav.emottak.ebms.ebxml.addressing
import no.nav.emottak.ebms.ebxml.createResponseHeader
import no.nav.emottak.ebms.ebxml.errorList
import no.nav.emottak.ebms.ebxml.messageHeader
import no.nav.emottak.ebms.validation.SignaturValidator
import no.nav.emottak.ebms.xml.ebMSSigning
import no.nav.emottak.ebms.xml.xmlMarshaller
import no.nav.emottak.melding.model.ErrorCode
import no.nav.emottak.melding.model.Feil
import no.nav.emottak.melding.model.SignatureDetails
import no.nav.emottak.util.marker
import no.nav.emottak.util.signatur.SignatureException
import org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.Error
import org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.ErrorList
import org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.MessageHeader
import org.slf4j.LoggerFactory
import org.w3c.dom.Document
import org.w3c.dom.Node
import org.xmlsoap.schemas.soap.envelope.Envelope
import java.lang.RuntimeException
import java.time.LocalDateTime

val log = LoggerFactory.getLogger("no.nav.emottak.ebms.model")

data class EbMSDocument(val requestId: String, val dokument: Document, val attachments: List<EbmsAttachment>) {

    private val envelope = lazy { xmlMarshaller.unmarshal(this.dokument) as Envelope }

    fun dokumentType(): DokumentType {
        if (attachments.size > 0) return DokumentType.PAYLOAD
        if (dokument.getElementsByTagNameNS(OASIS_EBXML_MSG_HEADER_XSD_NS_URI, "Acknowledgment")
            .item(0) != null
        ) {
            return DokumentType.ACKNOWLEDGMENT
        }
        if (dokument.getElementsByTagNameNS(OASIS_EBXML_MSG_HEADER_XSD_NS_URI, "ErrorList")
            .item(0) != null
        ) {
            return DokumentType.FAIL
        }
        throw RuntimeException("Unrecognized dokument type")
    }

    fun createFail(error: Error): EbmsMessageError {
        return createFail(ErrorList().also { it.error.add(error) })
    }

    fun createFail(errorList: ErrorList): EbmsMessageError {
        return EbmsMessageError(
            this.messageHeader()
                .createResponseHeader(newAction = MESSAGE_ERROR_ACTION, newService = EBMS_SERVICE_URI),
            errorList
        )
    }

    fun messageHeader(): MessageHeader {
        val node: Node =
            this.dokument.getElementsByTagNameNS(OASIS_EBXML_MSG_HEADER_XSD_NS_URI, OASIS_EBXML_MSG_HEADER_TAG).item(0)
        return xmlMarshaller.unmarshal(node)
    }

    fun transform(): EbmsMessage {
        val header = envelope.value.header!!
        val messageHeader = header.messageHeader()

        return when (dokumentType()) {
            DokumentType.PAYLOAD -> PayloadMessage(
                requestId,
                messageHeader.messageData.messageId,
                messageHeader.conversationId,
                messageHeader.cpaId!!,
                messageHeader.addressing(),
                attachments.first(),
                dokument,
                messageHeader.messageData.refToMessageId
            )

            DokumentType.FAIL -> {
                val errorList = header.errorList()!!.error.map {
                    Feil(ErrorCode.fromString(it.errorCode), it.description!!.value!!)
                }.toList()
                EbmsFail(
                    requestId,
                    messageHeader.messageData.messageId,
                    messageHeader.messageData.refToMessageId!!,
                    messageHeader.conversationId,
                    messageHeader.cpaId!!,
                    messageHeader.addressing(),
                    errorList,
                    dokument

                )
            }

            DokumentType.ACKNOWLEDGMENT -> {
                Acknowledgment(
                    requestId,
                    messageHeader.messageData.messageId,
                    header.acknowledgment()!!.refToMessageId,
                    messageHeader.conversationId,
                    messageHeader.cpaId!!,
                    messageHeader.addressing(),
                    dokument
                )
            }

            else -> throw RuntimeException("Unrecognized message type ${dokumentType()}")
        }
    }
}

enum class DokumentType {
    PAYLOAD, ACKNOWLEDGMENT, FAIL, STATUS, PING
}

fun EbMSDocument.signer(signatureDetails: SignatureDetails): EbMSDocument {
    try {
        ebMSSigning.sign(this, signatureDetails)
        return this
    } catch (e: Exception) {
        log.error(this.messageHeader().marker(), "Signering av ebms envelope feilet", e)
        throw SignatureException("Signering av ebms envelope feilet", e)
    }
}

fun EbMSDocument.sjekkSignature(signatureDetails: SignatureDetails) {
    SignaturValidator.validate(signatureDetails, this.dokument, this.attachments)
    log.info(this.messageHeader().marker(), "Signatur OK")
}

fun EbMSDocument.buildEbmMessage(): EbmsBaseMessage {
    val envelope: Envelope = xmlMarshaller.unmarshal(this.dokument)
    val header = envelope.header!!
    return if (header.acknowledgment() != null) {
        log.info(header.messageHeader().marker(), "Mottatt melding av type Acknowledgment")
        EbmsAcknowledgment(header.messageHeader(), header.acknowledgment()!!, this.dokument)
    } else if (header.errorList() != null) {
        log.info(header.messageHeader().marker(), "Mottatt melding av type ErrorList")
        EbmsMessageError(header.messageHeader(), header.errorList()!!, this.dokument)
    } else {
        log.info(header.messageHeader().marker(), "Mottatt melding av type payload")
        EbmsPayloadMessage(
            this.requestId,
            this.dokument,
            header.messageHeader(),
            header.ackRequested(),
            this.attachments,
            LocalDateTime.now()
        )
    }
}
