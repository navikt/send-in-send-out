package no.nav.emottak.util

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import no.nav.emottak.log
import no.nav.emottak.utils.common.model.PartyId
import no.nav.emottak.utils.common.model.SendInRequest
import no.nav.emottak.utils.common.model.SendInResponse
import no.nav.emottak.utils.common.parseOrGenerateUuid
import no.nav.emottak.utils.kafka.model.EbmsMessageDetail
import no.nav.emottak.utils.kafka.model.Event
import no.nav.emottak.utils.kafka.model.EventType
import no.nav.emottak.utils.kafka.service.EventLoggingService
import no.nav.emottak.utils.serialization.getErrorMessage
import java.time.Instant
import kotlin.uuid.Uuid

interface EventRegistrationService {
    fun registerEvent(
        eventType: EventType,
        requestId: Uuid,
        messageId: String,
        eventData: String = "{}"
    )

    fun registerEventMessageDetails(sendInRequest: SendInRequest, sendInResponse: SendInResponse)

    companion object {
        fun serializePartyId(partyIDs: List<PartyId>): String {
            val partyId = partyIDs.firstOrNull { it.type == "orgnummer" }
                ?: partyIDs.firstOrNull { it.type == "HER" }
                ?: partyIDs.firstOrNull { it.type == "ENH" }
                ?: partyIDs.first()

            return "${partyId.type}:${partyId.value}"
        }
    }
}

class EventRegistrationServiceImpl(
    private val eventLoggingService: EventLoggingService,
    private val scope: CoroutineScope
) : EventRegistrationService {

    override fun registerEvent(
        eventType: EventType,
        requestId: Uuid,
        messageId: String,
        eventData: String
    ) {
        val event = Event(
            eventType = eventType,
            requestId = requestId,
            contentId = "",
            messageId = messageId,
            eventData = eventData
        )
        log.debug("Registering event: $event")

        scope.launch {
            eventLoggingService.logEvent(event).onSuccess {
                log.debug("Event registered successfully")
            }.onFailure { e ->
                log.error("Error while registering event: ${Exception(e).getErrorMessage()}", e)
            }
        }
    }

    override fun registerEventMessageDetails(sendInRequest: SendInRequest, sendInResponse: SendInResponse) {
        log.debug("Registering message with requestId: ${sendInResponse.requestId}")

        val requestId = sendInResponse.requestId.parseOrGenerateUuid()

        val ebmsMessageDetail = EbmsMessageDetail(
            requestId = requestId,
            cpaId = sendInRequest.cpaId,
            conversationId = sendInResponse.conversationId,
            messageId = "",
            refToMessageId = sendInResponse.messageId,
            fromPartyId = EventRegistrationService.serializePartyId(sendInResponse.addressing.from.partyId),
            fromRole = sendInResponse.addressing.from.role,
            toPartyId = EventRegistrationService.serializePartyId(sendInResponse.addressing.to.partyId),
            toRole = sendInResponse.addressing.to.role,
            service = sendInResponse.addressing.service,
            action = sendInResponse.addressing.action,
            refParam = null,
            sender = null,
            sentAt = Instant.now()
        )
        log.debug("Publishing message details: $ebmsMessageDetail")

        scope.launch {
            eventLoggingService.logMessageDetails(ebmsMessageDetail).onSuccess {
                log.debug("Message details published successfully")
            }.onFailure { e ->
                log.error("Error while registering message details: ${Exception(e).getErrorMessage()}", e)
            }
        }
    }
}

class EventRegistrationServiceFake : EventRegistrationService {
    override fun registerEvent(
        eventType: EventType,
        requestId: Uuid,
        messageId: String,
        eventData: String
    ) {
        log.debug("Registering event $eventType for requestId: $requestId, messageId: $messageId and eventData: $eventData")
    }

    override fun registerEventMessageDetails(sendInRequest: SendInRequest, sendInResponse: SendInResponse) {
        log.debug("Registering message details for SendInResponse: $sendInResponse")
    }
}
