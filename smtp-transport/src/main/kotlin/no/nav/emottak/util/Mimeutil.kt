package no.nav.emottak.util

import no.nav.emottak.smtp.EmailMsg
import no.nav.emottak.smtp.Part

private const val MESSAGE_ID = "Message-Id"
private const val SINGLE_PART = "singlepart"
private const val MULTI_PART = "multipart"
private const val CONTENT_TYPE = "Content-Type"

data class Payload(
    val messageId: String,
    val contentType: String,
    val content: ByteArray
)

fun EmailMsg.getMessageIdSinglePart() = getMessageIdWithPart(SINGLE_PART)

fun EmailMsg.getMessageIdMultiPart() = getMessageIdWithPart(MULTI_PART)

fun EmailMsg.getFirstPartAsBytes() = parts.first().bytes

fun EmailMsg.getLastPartsAsPayloads() = parts
    .drop(1)
    .map {
        Payload(
            this.getMessageId(),
            it.getContentType(),
            it.bytes
        )
    }

private fun Part.getContentType() = "${headers[CONTENT_TYPE]}"

private fun EmailMsg.getMessageId() = "${headers[MESSAGE_ID]}"

private fun EmailMsg.getMessageIdWithPart(part: String) = "${headers[MESSAGE_ID]}:$part"
