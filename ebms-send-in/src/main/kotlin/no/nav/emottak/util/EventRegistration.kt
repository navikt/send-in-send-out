package no.nav.emottak.util

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import no.nav.emottak.ebms.log
import no.nav.emottak.melding.model.SendInRequest
import no.nav.emottak.melding.model.SendInResponse
import no.nav.emottak.utils.common.parseOrGenerateUuid
import no.nav.emottak.utils.kafka.model.Event
import no.nav.emottak.utils.kafka.model.EventType
import no.nav.emottak.utils.kafka.service.EventLoggingService
import no.nav.emottak.utils.serialization.getErrorMessage
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
private fun EventLoggingService.publishEvent(
    eventType: EventType,
    requestId: Uuid,
    messageId: String,
    eventData: String,
    scope: CoroutineScope
) {
    val event = Event(
        eventType = eventType,
        requestId = requestId,
        contentId = "",
        messageId = messageId,
        eventData = eventData
    )
    log.debug("Publishing event: $event")

    scope.launch {
        this@publishEvent.logEvent(event).onSuccess {
            log.debug("Event published successfully")
        }.onFailure { e ->
            log.error("Error while publishing event: ${Exception(e).getErrorMessage()}", e)
        }
    }
}

@OptIn(ExperimentalUuidApi::class)
fun EventLoggingService.registerEvent(
    eventType: EventType,
    sendInResponse: SendInResponse,
    eventData: String = "",
    scope: CoroutineScope
) {
    log.debug("Registering event: $eventType, $sendInResponse")
    try {
        val requestId = sendInResponse.requestId.parseOrGenerateUuid()
        publishEvent(eventType, requestId, "", eventData, scope)
    } catch (e: Exception) {
        log.error("Error while registering event: ${e.getErrorMessage()}", e)
    }
}

@OptIn(ExperimentalUuidApi::class)
fun EventLoggingService.registerEvent(
    eventType: EventType,
    sendInRequest: SendInRequest,
    eventData: String = "",
    scope: CoroutineScope
) {
    log.debug("Registering event: $eventType, $SendInRequest")
    try {
        val requestId = sendInRequest.requestId.parseOrGenerateUuid()
        publishEvent(eventType, requestId, sendInRequest.messageId, eventData, scope)
    } catch (e: Exception) {
        log.error("Error while registering event: ${e.getErrorMessage()}", e)
    }
}
