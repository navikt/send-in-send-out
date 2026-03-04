package no.nav.emottak.ebms.service

import arrow.core.Either
import arrow.core.raise.Raise
import arrow.core.raise.either
import io.micrometer.core.instrument.MeterRegistry
import kotlinx.serialization.json.Json
import no.nav.emottak.ebms.utils.SupportedServiceType
import no.nav.emottak.ebms.utils.SupportedServiceType.Companion.toSupportedService
import no.nav.emottak.ebms.utils.timed
import no.nav.emottak.fellesformat.FellesFormatXmlMarshaller
import no.nav.emottak.fellesformat.asEIFellesFormat
import no.nav.emottak.fellesformat.asEIFellesFormatWithFrikort
import no.nav.emottak.frikort.egenandelForesporselXmlMarshaller
import no.nav.emottak.frikort.frikortsporring
import no.nav.emottak.frikort.frikortsporringMengde
import no.nav.emottak.frikort.rest.postHarBorgerEgenandelfritak
import no.nav.emottak.frikort.rest.postHarBorgerFrikort
import no.nav.emottak.frikort.rest.toFrikortsporringRequest
import no.nav.emottak.frikort.rest.toMsgHead
import no.nav.emottak.pasientliste.PasientlisteService
import no.nav.emottak.trekkopplysning.TrekkopplysningService
import no.nav.emottak.utbetaling.UtbetalingClient
import no.nav.emottak.utbetaling.UtbetalingXmlMarshaller
import no.nav.emottak.util.EventRegistrationService
import no.nav.emottak.util.LogLevel
import no.nav.emottak.util.asJson
import no.nav.emottak.util.asXml
import no.nav.emottak.util.extractReferenceParameter
import no.nav.emottak.utils.common.model.SendInRequest
import no.nav.emottak.utils.common.model.SendInResponse
import no.nav.emottak.utils.common.parseOrGenerateUuid
import no.nav.emottak.utils.environment.isProdEnv
import no.nav.emottak.utils.kafka.model.EventDataType
import no.nav.emottak.utils.kafka.model.EventType
import org.slf4j.LoggerFactory
import kotlin.uuid.Uuid

object FagmeldingService {
    private val log = LoggerFactory.getLogger("no.nav.emottak.ebms.service.FagmeldingService")

    suspend fun processRequest(
        sendInRequest: SendInRequest,
        meterRegistry: MeterRegistry,
        eventRegistrationService: EventRegistrationService,
        trekkopplysningService: TrekkopplysningService
    ): Either<Throwable, SendInResponse> = either {
        when (sendInRequest.addressing.service.toSupportedService()) {
            SupportedServiceType.Inntektsforesporsel ->
                timed(meterRegistry, "Inntektsforesporsel") {
                    getInntektsforesporsel(sendInRequest, eventRegistrationService).also {
                        persistEventsAndMessageDetails(eventRegistrationService, sendInRequest, it)
                    }
                }

            SupportedServiceType.HarBorgerEgenandelFritak ->
                when (sendInRequest.sendToRESTFrikortEndpoint()) {
                    true -> getHarBorgerEgenandelFritakREST(sendInRequest)
                    false -> {
                        timed(meterRegistry, "frikort-sporing") {
                            getHarBorgerFrikort(sendInRequest)
                        }
                    }
                }

            SupportedServiceType.HarBorgerFrikort ->
                when (sendInRequest.sendToRESTFrikortEndpoint()) {
                    true -> getHarBorgerFrikortREST(sendInRequest)
                    false -> {
                        timed(meterRegistry, "frikort-sporing") {
                            getHarBorgerFrikort(sendInRequest)
                        }
                    }
                }

            SupportedServiceType.HarBorgerFrikortMengde ->
                timed(meterRegistry, "frikortMengde-sporing") {
                    getHarBorgerFrikortMengde(sendInRequest, eventRegistrationService).also {
                        persistEventsAndMessageDetails(eventRegistrationService, sendInRequest, it)
                    }
                }

            SupportedServiceType.PasientlisteForesporsel ->
                timed(meterRegistry, "PasientlisteForesporsel") {
                    getPasientlisteForesporsel(sendInRequest, eventRegistrationService)
                }

            SupportedServiceType.Trekkopplysning ->
                timed(meterRegistry, "Trekkopplysning") {
                    putTrekkopplysning(sendInRequest, eventRegistrationService, trekkopplysningService).also {
                        persistEventsAndMessageDetails(eventRegistrationService, sendInRequest, it)
                    }
                }

            SupportedServiceType.Unsupported ->
                throw NotImplementedError(
                    "Service: ${sendInRequest.addressing.service} is not implemented"
                )
        }
    }

    private fun persistEventsAndMessageDetails(eventRegistrationService: EventRegistrationService, sendInRequest: SendInRequest, sendInResponse: SendInResponse) {
        eventRegistrationService.registerEventMessageDetails(sendInRequest, sendInResponse)
        eventRegistrationService.registerEvent(
            EventType.MESSAGE_RECEIVED_FROM_FAGSYSTEM,
            sendInResponse.requestId.parseOrGenerateUuid(),
            ""
        )
    }

    private fun getPasientlisteForesporsel(
        sendInRequest: SendInRequest,
        eventRegistrationService: EventRegistrationService
    ): SendInResponse {
        if (isProdEnv()) {
            throw NotImplementedError(
                "PasientlisteForesporsel is used in prod. Feature is not ready. Aborting."
            )
        }
        return with(sendInRequest.asEIFellesFormat()) {
            persistReferenceParameter(sendInRequest, this.extractReferenceParameter(), eventRegistrationService)
            log.asXml(LogLevel.DEBUG, "Wrapped message (fellesformatRequest)", this, FellesFormatXmlMarshaller)
            PasientlisteService.pasientlisteForesporsel(this).also {
                eventRegistrationService.registerEvent(
                    EventType.MESSAGE_SENT_TO_FAGSYSTEM,
                    sendInRequest.requestId.parseOrGenerateUuid(),
                    sendInRequest.messageId
                )
            }.let { fellesformatResponse ->
                SendInResponse(
                    messageId = Uuid.random().toString(),
                    conversationId = sendInRequest.conversationId,
                    addressing = sendInRequest.addressing.replyTo(
                        fellesformatResponse.mottakenhetBlokk.ebService,
                        fellesformatResponse.mottakenhetBlokk.ebAction
                    ),
                    payload = FellesFormatXmlMarshaller.marshalToByteArray(
                        fellesformatResponse.appRec
                    ),
                    requestId = Uuid.random().toString()
                ).also {
                    log.asJson(
                        LogLevel.DEBUG,
                        "Sending SendInResponse",
                        it,
                        SendInResponse.serializer()
                    )
                }
            }
        }
    }

    private fun Raise<Throwable>.getHarBorgerFrikortMengde(
        sendInRequest: SendInRequest,
        eventRegistrationService: EventRegistrationService
    ): SendInResponse = Either.catch {
        with(sendInRequest.asEIFellesFormat()) {
            persistReferenceParameter(sendInRequest, this.extractReferenceParameter(), eventRegistrationService)
            frikortsporringMengde(this).also {
                eventRegistrationService.registerEvent(
                    EventType.MESSAGE_SENT_TO_FAGSYSTEM,
                    sendInRequest.requestId.parseOrGenerateUuid(),
                    sendInRequest.messageId
                )
            }
        }
    }.bind().let { response ->
        SendInResponse(
            messageId = Uuid.random().toString(),
            conversationId = sendInRequest.conversationId,
            addressing = sendInRequest.addressing.replyTo(
                response.eiFellesformat.mottakenhetBlokk.ebService,
                response.eiFellesformat.mottakenhetBlokk.ebAction
            ),
            payload = FellesFormatXmlMarshaller.marshalToByteArray(
                response.eiFellesformat.msgHead
            ),
            requestId = Uuid.random().toString()
        )
    }

    private suspend fun Raise<Throwable>.getHarBorgerFrikortREST(
        sendInRequest: SendInRequest
    ): SendInResponse = Either.catch {
        log.info("Message forwarded to HarBorgerFrikort REST")
        with(sendInRequest.asEIFellesFormatWithFrikort()) {
            log.info("Refparam: ${this.extractReferenceParameter()}")
            postHarBorgerFrikort(this.toFrikortsporringRequest())
        }
    }.bind().let { response ->
        log.debug("Marshalled response from new frikort: ${egenandelForesporselXmlMarshaller.marshal(response.eiFellesformat.msgHead.toMsgHead())}")
        SendInResponse(
            messageId = Uuid.random().toString(),
            conversationId = sendInRequest.conversationId,
            addressing = sendInRequest.addressing.replyTo(
                response.eiFellesformat.mottakenhetBlokk.ebService!!.value,
                response.eiFellesformat.mottakenhetBlokk.ebAction!!
            ),
            payload = egenandelForesporselXmlMarshaller.marshalToByteArray(
                response.eiFellesformat.msgHead.toMsgHead()
            ),
            requestId = Uuid.random().toString()
        )
    }

    private suspend fun Raise<Throwable>.getHarBorgerEgenandelFritakREST(
        sendInRequest: SendInRequest
    ): SendInResponse = Either.catch {
        log.info("Message forwarded to HarBorgerEgenandelFritak REST")
        with(sendInRequest.asEIFellesFormatWithFrikort()) {
            log.info("Refparam: ${this.extractReferenceParameter()}")
            postHarBorgerEgenandelfritak(this.toFrikortsporringRequest())
        }
    }.bind().let { response ->
        log.debug("Marshalled response from new frikort: ${egenandelForesporselXmlMarshaller.marshal(response.eiFellesformat.msgHead.toMsgHead())}")
        SendInResponse(
            messageId = Uuid.random().toString(),
            conversationId = sendInRequest.conversationId,
            addressing = sendInRequest.addressing.replyTo(
                response.eiFellesformat.mottakenhetBlokk.ebService!!.value,
                response.eiFellesformat.mottakenhetBlokk.ebAction!!
            ),
            payload = egenandelForesporselXmlMarshaller.marshalToByteArray(
                response.eiFellesformat.msgHead.toMsgHead()
            ),
            requestId = Uuid.random().toString()
        )
    }

    private fun Raise<Throwable>.getHarBorgerFrikort(
        sendInRequest: SendInRequest
    ): SendInResponse = Either.catch {
        with(sendInRequest.asEIFellesFormat()) {
            log.info("Refparam: ${this.extractReferenceParameter()}")
            frikortsporring(this)
        }
    }.bind().let { response ->
        SendInResponse(
            messageId = Uuid.random().toString(),
            conversationId = sendInRequest.conversationId,
            addressing = sendInRequest.addressing.replyTo(
                response.eiFellesformat.mottakenhetBlokk.ebService,
                response.eiFellesformat.mottakenhetBlokk.ebAction
            ),
            payload = FellesFormatXmlMarshaller.marshalToByteArray(
                response.eiFellesformat.msgHead
            ),
            requestId = Uuid.random().toString()
        )
    }

    private fun Raise<Throwable>.getInntektsforesporsel(
        sendInRequest: SendInRequest,
        eventRegistrationService: EventRegistrationService
    ): SendInResponse = Either.catch {
        UtbetalingClient.behandleInntektsforesporsel(
            sendInRequest.payload
        ).also {
            eventRegistrationService.registerEvent(
                EventType.MESSAGE_SENT_TO_FAGSYSTEM,
                sendInRequest.requestId.parseOrGenerateUuid(),
                sendInRequest.messageId
            )
        }
    }.bind().let { msgHeadResponse ->
        SendInResponse(
            messageId = Uuid.random().toString(),
            conversationId = sendInRequest.conversationId,
            addressing = sendInRequest.addressing.replyTo(
                sendInRequest.addressing.service,
                msgHeadResponse.msgInfo.type.v
            ),
            payload = UtbetalingXmlMarshaller.marshalToByteArray(msgHeadResponse),
            requestId = Uuid.random().toString()
        )
    }

    private fun Raise<Throwable>.putTrekkopplysning(
        sendInRequest: SendInRequest,
        eventRegistrationService: EventRegistrationService,
        trekkopplysningService: TrekkopplysningService
    ): SendInResponse = Either.catch {
        with(sendInRequest.asEIFellesFormat()) {
            persistReferenceParameter(sendInRequest, this.extractReferenceParameter(), eventRegistrationService)
            trekkopplysningService.trekkopplysning(this).also {
                eventRegistrationService.registerEvent(
                    EventType.MESSAGE_SENT_TO_FAGSYSTEM,
                    sendInRequest.requestId.parseOrGenerateUuid(),
                    sendInRequest.messageId
                )
            }
        }
    }
        .bind().let { response ->
            SendInResponse(
                messageId = Uuid.random().toString(),
                conversationId = sendInRequest.conversationId,
                addressing = sendInRequest.addressing.replyTo(
                    sendInRequest.addressing.service,
                    "" // response.eiFellesformat.mottakenhetBlokk.ebAction
                ),
                payload = FellesFormatXmlMarshaller.marshalToByteArray(
                    "".toByteArray()
                ),
                requestId = Uuid.random().toString()
            )
        }

    private fun persistReferenceParameter(
        sendInRequest: SendInRequest,
        referenceParameter: String,
        eventRegistrationService: EventRegistrationService
    ) {
        log.info("Refparam: $referenceParameter")

        val eventData = Json.encodeToString(
            mapOf(EventDataType.REFERENCE_PARAMETER.value to referenceParameter)
        )
        eventRegistrationService.registerEvent(
            EventType.REFERENCE_RETRIEVED,
            sendInRequest.requestId.parseOrGenerateUuid(),
            sendInRequest.messageId,
            eventData
        )
    }
}

/**
 * Current forwarding rules:
 *  Forward nav:70079 to WS endpoint due to external bug
 *  Forward every other CPA to REST endpoint
 */
private fun SendInRequest.sendToRESTFrikortEndpoint(): Boolean = cpaId != "nav:70079"
