package no.nav.emottak.util

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import no.nav.emottak.log
import no.nav.emottak.utils.common.model.PartyId
import no.nav.emottak.utils.common.model.SendInRequest
import no.nav.emottak.utils.common.model.SendInResponse
import no.nav.emottak.utils.common.parseOrGenerateUuid
import no.nav.emottak.utils.kafka.model.EbmsMessageDetail
import no.nav.emottak.utils.kafka.model.Event
import no.nav.emottak.utils.kafka.model.EventDataType
import no.nav.emottak.utils.kafka.model.EventType
import no.nav.emottak.utils.kafka.service.EventLoggingService
import no.nav.emottak.utils.serialization.getErrorMessage
import no.nav.emottak.utils.serialization.toEventDataJson
import java.time.Instant
import kotlin.uuid.Uuid

interface EventRegistrationService {
    fun registerEvent(
        eventType: EventType,
        requestId: Uuid,
        messageId: String,
        eventData: String = "{}",
        conversationId: String? = null
    )

    fun registerEventMessageDetails(sendInResponse: SendInResponse)
    fun registerErrorOnHandoverToFagsystem(sendInRequest: SendInRequest, error: Throwable)
    fun registerMessageSentToFagsystem(sendInRequest: SendInRequest, endpointId: String)
    fun registerReferenceParameter(sendInRequest: SendInRequest, referenceParameter: String)
    fun registerMessageReceivedFromFagsystem(sendInResponse: SendInResponse)

    companion object {
        fun serializePartyId(partyIDs: List<PartyId>): String {
            val partyId = partyIDs.firstOrNull { it.type == "orgnummer" }
                ?: partyIDs.firstOrNull { it.type == "HER" }
                ?: partyIDs.firstOrNull { it.type == "ENH" }
                ?: partyIDs.firstOrNull()

            if (partyId == null) {
                return ""
            }
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
        eventData: String,
        conversationId: String?
    ) {
        val event = Event(
            eventType = eventType,
            requestId = requestId,
            contentId = "",
            messageId = messageId,
            eventData = eventData
        )
        log.debug("Registering event: {}", event)

        scope.launch {
            runCatching {
                eventLoggingService.logEvent(event)
            }.onSuccess {
                log.debug("Event registered successfully")
            }.onFailure { e ->
                log.error("Error while registering event: ${Exception(e).getErrorMessage()}", e)
            }
        }
    }

    override fun registerEventMessageDetails(sendInResponse: SendInResponse) {
        log.debug("Registering message with requestId: ${sendInResponse.requestId}")

        val requestId = sendInResponse.requestId.parseOrGenerateUuid()

        val ebmsMessageDetail = EbmsMessageDetail(
            requestId = requestId,
            cpaId = sendInResponse.cpaId,
            conversationId = sendInResponse.conversationId,
            messageId = sendInResponse.messageId,
            refToMessageId = sendInResponse.refToMessageId,
            fromPartyId = EventRegistrationService.serializePartyId(sendInResponse.addressing.from.partyId),
            fromRole = sendInResponse.addressing.from.role,
            toPartyId = EventRegistrationService.serializePartyId(sendInResponse.addressing.to.partyId),
            toRole = sendInResponse.addressing.to.role,
            service = sendInResponse.addressing.service,
            action = sendInResponse.addressing.action,
            sentAt = Instant.now() // TODO: Burde vært nowOsloToInstant()?
        )
        log.debug("Publishing message details: {}", ebmsMessageDetail)

        scope.launch {
            runCatching {
                eventLoggingService.logMessageDetails(ebmsMessageDetail)
            }.onSuccess {
                log.debug("Message details published successfully")
            }.onFailure { e ->
                log.error("Error while registering message details: ${Exception(e).getErrorMessage()}", e)
            }
        }
    }

    override fun registerErrorOnHandoverToFagsystem(
        sendInRequest: SendInRequest,
        error: Throwable
    ) {
        registerEvent(
            eventType = EventType.ERROR_WHILE_SENDING_MESSAGE_TO_FAGSYSTEM,
            requestId = sendInRequest.requestId.parseOrGenerateUuid(),
            messageId = sendInRequest.messageId,
            eventData = Exception(error).toEventDataJson(),
            conversationId = sendInRequest.conversationId
        )
    }

    override fun registerMessageSentToFagsystem(sendInRequest: SendInRequest, endpointId: String) {
        registerEvent(
            EventType.MESSAGE_SENT_TO_FAGSYSTEM,
            sendInRequest.requestId.parseOrGenerateUuid(),
            sendInRequest.messageId,
            encodeToJsonString(EventDataType.QUEUE_NAME.value to endpointId),
            sendInRequest.conversationId
        )
    }

    override fun registerReferenceParameter(
        sendInRequest: SendInRequest,
        referenceParameter: String
    ) {
        registerEvent(
            EventType.REFERENCE_RETRIEVED,
            requestId = sendInRequest.requestId.parseOrGenerateUuid(),
            messageId = sendInRequest.messageId,
            eventData = encodeToJsonString(EventDataType.REFERENCE_PARAMETER.value to referenceParameter),
            conversationId = sendInRequest.conversationId
        )
    }

    override fun registerMessageReceivedFromFagsystem(sendInResponse: SendInResponse) = registerEvent(
        EventType.MESSAGE_RECEIVED_FROM_FAGSYSTEM,
        requestId = sendInResponse.requestId.parseOrGenerateUuid(),
        messageId = sendInResponse.messageId,
        eventData = "{}",
        conversationId = sendInResponse.conversationId
    )
}

class EventRegistrationServiceFake : EventRegistrationService {
    override fun registerEvent(
        eventType: EventType,
        requestId: Uuid,
        messageId: String,
        eventData: String,
        conversationId: String?
    ) {
        log.debug(
            "Registering event {} for requestId: {}, messageId: {}, conversationId: {} and eventData: {}",
            eventType,
            requestId,
            messageId,
            conversationId,
            eventData
        )
    }

    override fun registerEventMessageDetails(sendInResponse: SendInResponse) {
        log.debug("Registering message details for SendInResponse: {}", sendInResponse)
    }

    override fun registerErrorOnHandoverToFagsystem(
        sendInRequest: SendInRequest,
        error: Throwable
    ) {
        log.info(
            "Registering error on handover to fagsystem for requestId: {}, messageId: {}, conversationId: {} and error: {}",
            sendInRequest.requestId,
            sendInRequest.messageId,
            sendInRequest.conversationId,
            Exception(error).getErrorMessage()
        )
    }

    override fun registerMessageSentToFagsystem(
        sendInRequest: SendInRequest,
        endpointId: String
    ) {
        log.info(
            "Registering message sent to fagsystem for requestId: {}, messageId: {}, conversationId: {} and endpointId: {}",
            sendInRequest.requestId,
            sendInRequest.messageId,
            sendInRequest.conversationId,
            endpointId
        )
    }

    override fun registerReferenceParameter(
        sendInRequest: SendInRequest,
        referenceParameter: String
    ) {
        log.info(
            "Registering reference parameter for requestId: {}, messageId: {}, conversationId: {} and referenceParameter: {}",
            sendInRequest.requestId,
            sendInRequest.messageId,
            sendInRequest.conversationId,
            referenceParameter
        )
    }

    override fun registerMessageReceivedFromFagsystem(sendInResponse: SendInResponse) {
        log.info(
            "Registering message received from fagsystem for requestId: {}, messageId: {}, conversationId: {}",
            sendInResponse.requestId,
            sendInResponse.messageId,
            sendInResponse.conversationId
        )
    }
}

fun encodeToJsonString(vararg pairs: Pair<String, String>): String = Json.encodeToString(mapOf(*pairs))
