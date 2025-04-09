package no.nav.emottak.ebms.utils.extensions

import kotlinx.coroutines.slf4j.MDCContext
import no.nav.emottak.melding.model.SendInRequest

fun SendInRequest.mdcContext(): MDCContext = MDCContext(
    mapOf(
        "messageId" to messageId,
        "conversationId" to conversationId
    )
)
