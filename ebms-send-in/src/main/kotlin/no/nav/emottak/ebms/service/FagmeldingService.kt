package no.nav.emottak.ebms.service

import arrow.core.Either
import arrow.core.raise.Raise
import arrow.core.raise.either
import io.micrometer.core.instrument.MeterRegistry
import kotlinx.serialization.json.Json
import no.nav.emottak.ebms.utils.SupportedAsyncServiceType
import no.nav.emottak.ebms.utils.SupportedAsyncServiceType.Companion.toSupportedAsyncService
import no.nav.emottak.ebms.utils.SupportedServiceType
import no.nav.emottak.ebms.utils.SupportedServiceType.Companion.toSupportedService
import no.nav.emottak.ebms.utils.timed
import no.nav.emottak.fellesformat.FellesFormatXmlMarshaller
import no.nav.emottak.fellesformat.asEIFellesFormat
import no.nav.emottak.fellesformat.asEIFellesFormatWithFrikort
import no.nav.emottak.fellesformat.asEIFellesFormat_Trekkopplysning
import no.nav.emottak.frikort.egenandelForesporselXmlMarshaller
import no.nav.emottak.frikort.frikortsporring
import no.nav.emottak.frikort.frikortsporringMengde
import no.nav.emottak.frikort.rest.postHarBorgerEgenandelfritak
import no.nav.emottak.frikort.rest.postHarBorgerFrikort
import no.nav.emottak.frikort.rest.toFrikortsporringRequest
import no.nav.emottak.frikort.rest.toMsgHead
import no.nav.emottak.trekkopplysning.TrekkopplysningService
import no.nav.emottak.utbetaling.UtbetalingClient
import no.nav.emottak.utbetaling.UtbetalingXmlMarshaller
import no.nav.emottak.util.EventRegistrationService
import no.nav.emottak.util.extractReferenceParameter
import no.nav.emottak.utils.common.model.SendInRequest
import no.nav.emottak.utils.common.model.SendInResponse
import no.nav.emottak.utils.common.parseOrGenerateUuid
import no.nav.emottak.utils.kafka.model.EventDataType
import no.nav.emottak.utils.kafka.model.EventType
import no.trygdeetaten.xml.eiff._1.EIFellesformat
import org.slf4j.LoggerFactory
import kotlin.uuid.Uuid

object FagmeldingService {
    private val log = LoggerFactory.getLogger("no.nav.emottak.ebms.service.FagmeldingService")

    suspend fun processRequestSynchronously(
        sendInRequest: SendInRequest,
        meterRegistry: MeterRegistry,
        eventRegistrationService: EventRegistrationService
    ): Either<Throwable, SendInResponse> = either {
        when (sendInRequest.addressing.service.toSupportedService()) {
            SupportedServiceType.Inntektsforesporsel ->
                timed(meterRegistry, "Inntektsforesporsel") {
                    log.info("Inntektsforesporsel is processed synchronously")
                    getInntektsforesporsel(sendInRequest, eventRegistrationService)
                }

            SupportedServiceType.HarBorgerEgenandelFritak ->
                when (sendInRequest.sendToRESTFrikortEndpoint()) {
                    true -> {
                        log.info("HarBorgerEgenandelFritak is processed synchronously, via REST")
                        getHarBorgerEgenandelFritakREST(sendInRequest, eventRegistrationService)
                    }
                    false -> {
                        log.info("HarBorgerEgenandelFritak is processed synchronously")
                        timed(meterRegistry, "frikort-sporing") {
                            getHarBorgerFrikort(sendInRequest, eventRegistrationService)
                        }
                    }
                }

            SupportedServiceType.HarBorgerFrikort ->
                when (sendInRequest.sendToRESTFrikortEndpoint()) {
                    true -> {
                        log.info("HarBorgerFrikort is processed synchronously, via REST")
                        getHarBorgerFrikortREST(sendInRequest, eventRegistrationService)
                    }
                    false -> {
                        timed(meterRegistry, "frikort-sporing") {
                            log.info("HarBorgerFrikort is processed synchronously")
                            getHarBorgerFrikort(sendInRequest, eventRegistrationService)
                        }
                    }
                }

            SupportedServiceType.HarBorgerFrikortMengde ->
                timed(meterRegistry, "frikortMengde-sporing") {
                    log.info("HarBorgerFrikortMengde is processed synchronously")
                    getHarBorgerFrikortMengde(sendInRequest, eventRegistrationService)
                }

            SupportedServiceType.Unsupported ->
                throw NotImplementedError(
                    "Service: ${sendInRequest.addressing.service} is not implemented"
                )
        }.also {
            eventRegistrationService.registerEventMessageDetails(it)
        }.also {
            eventRegistrationService.registerEvent(
                EventType.MESSAGE_RECEIVED_FROM_FAGSYSTEM,
                requestId = it.requestId.parseOrGenerateUuid(),
                messageId = "",
                conversationId = it.conversationId
            )
        }
    }

    suspend fun processRequestAsynchronously(
        sendInRequest: SendInRequest,
        meterRegistry: MeterRegistry,
        eventRegistrationService: EventRegistrationService,
        trekkopplysningService: TrekkopplysningService
    ): Either<Throwable, Unit> = either {
        when (sendInRequest.addressing.service.toSupportedAsyncService()) {
            SupportedAsyncServiceType.Trekkopplysning ->
                timed(meterRegistry, "Trekkopplysning") {
                    log.info("Trekkopplysning is processed asynchronously")
                    sendTrekkopplysning(sendInRequest, eventRegistrationService, trekkopplysningService)
                }
            SupportedAsyncServiceType.Unsupported ->
                throw NotImplementedError(
                    "Service: ${sendInRequest.addressing.service} is not implemented"
                )
        }
        // Logging av eventer som krever respons ligger i Receiveren som mottar respons/fellesformat fra fagsystem
    }

    private fun Raise<Throwable>.getHarBorgerFrikortMengde(
        sendInRequest: SendInRequest,
        eventRegistrationService: EventRegistrationService
    ): SendInResponse = Either.catch {
        with(sendInRequest.asEIFellesFormat()) {
            extractReferenceParameter(sendInRequest, this, eventRegistrationService)
            frikortsporringMengde(this).also {
                eventRegistrationService.registerEvent(
                    EventType.MESSAGE_SENT_TO_FAGSYSTEM,
                    requestId = sendInRequest.requestId.parseOrGenerateUuid(),
                    messageId = sendInRequest.messageId,
                    conversationId = sendInRequest.conversationId
                )
            }
        }
    }.bind().let { response ->
        SendInResponse(
            messageId = Uuid.random().toString(),
            refToMessageId = sendInRequest.messageId,
            conversationId = sendInRequest.conversationId,
            cpaId = sendInRequest.cpaId,
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
        sendInRequest: SendInRequest,
        eventRegistrationService: EventRegistrationService
    ): SendInResponse = Either.catch {
        with(sendInRequest.asEIFellesFormatWithFrikort()) {
            extractReferenceParameter(sendInRequest, this, eventRegistrationService)
            postHarBorgerFrikort(this.toFrikortsporringRequest()).also {
                eventRegistrationService.registerEvent(
                    EventType.MESSAGE_SENT_TO_FAGSYSTEM,
                    requestId = sendInRequest.requestId.parseOrGenerateUuid(),
                    messageId = sendInRequest.messageId,
                    conversationId = sendInRequest.conversationId
                )
            }
        }
    }.bind().let { response ->
        log.debug("Marshalled response from new frikort: ${egenandelForesporselXmlMarshaller.marshal(response.eiFellesformat.msgHead.toMsgHead())}")
        SendInResponse(
            messageId = Uuid.random().toString(),
            refToMessageId = sendInRequest.messageId,
            conversationId = sendInRequest.conversationId,
            cpaId = sendInRequest.cpaId,
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
        sendInRequest: SendInRequest,
        eventRegistrationService: EventRegistrationService
    ): SendInResponse = Either.catch {
        with(sendInRequest.asEIFellesFormatWithFrikort()) {
            extractReferenceParameter(sendInRequest, this, eventRegistrationService)
            postHarBorgerEgenandelfritak(this.toFrikortsporringRequest()).also {
                eventRegistrationService.registerEvent(
                    EventType.MESSAGE_SENT_TO_FAGSYSTEM,
                    requestId = sendInRequest.requestId.parseOrGenerateUuid(),
                    messageId = sendInRequest.messageId,
                    conversationId = sendInRequest.conversationId
                )
            }
        }
    }.bind().let { response ->
        log.debug("Marshalled response from new frikort: ${egenandelForesporselXmlMarshaller.marshal(response.eiFellesformat.msgHead.toMsgHead())}")
        SendInResponse(
            messageId = Uuid.random().toString(),
            refToMessageId = sendInRequest.messageId,
            conversationId = sendInRequest.conversationId,
            cpaId = sendInRequest.cpaId,
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
        sendInRequest: SendInRequest,
        eventRegistrationService: EventRegistrationService
    ): SendInResponse = Either.catch {
        with(sendInRequest.asEIFellesFormat()) {
            extractReferenceParameter(sendInRequest, this, eventRegistrationService)
            frikortsporring(this).also {
                eventRegistrationService.registerEvent(
                    EventType.MESSAGE_SENT_TO_FAGSYSTEM,
                    requestId = sendInRequest.requestId.parseOrGenerateUuid(),
                    messageId = sendInRequest.messageId,
                    conversationId = sendInRequest.conversationId
                )
            }
        }
    }.bind().let { response ->
        SendInResponse(
            messageId = Uuid.random().toString(),
            refToMessageId = sendInRequest.messageId,
            conversationId = sendInRequest.conversationId,
            cpaId = sendInRequest.cpaId,
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
                requestId = sendInRequest.requestId.parseOrGenerateUuid(),
                messageId = sendInRequest.messageId,
                conversationId = sendInRequest.conversationId
            )
        }
    }.bind().let { msgHeadResponse ->
        SendInResponse(
            messageId = Uuid.random().toString(),
            refToMessageId = sendInRequest.messageId,
            conversationId = sendInRequest.conversationId,
            cpaId = sendInRequest.cpaId,
            addressing = sendInRequest.addressing.replyTo(
                sendInRequest.addressing.service,
                msgHeadResponse.msgInfo.type.v
            ),
            payload = UtbetalingXmlMarshaller.marshalToByteArray(msgHeadResponse),
            requestId = Uuid.random().toString()
        )
    }

    private fun Raise<Throwable>.sendTrekkopplysning(
        sendInRequest: SendInRequest,
        eventRegistrationService: EventRegistrationService,
        trekkopplysningService: TrekkopplysningService
    ) = Either.catch {
        with(sendInRequest.asEIFellesFormat_Trekkopplysning()) {
            trekkopplysningService.trekkopplysning(this).also {
                eventRegistrationService.registerEvent(
                    EventType.MESSAGE_SENT_TO_FAGSYSTEM,
                    sendInRequest.requestId.parseOrGenerateUuid(),
                    sendInRequest.messageId
                )
            }
        }
    }.bind()

    private fun extractReferenceParameter(
        sendInRequest: SendInRequest,
        fellesformat: EIFellesformat,
        eventRegistrationService: EventRegistrationService
    ) {
        val referenceParameter = fellesformat.extractReferenceParameter()
        log.info("Refparam: $referenceParameter")

        val eventData = Json.encodeToString(
            mapOf(EventDataType.REFERENCE_PARAMETER.value to referenceParameter)
        )
        eventRegistrationService.registerEvent(
            EventType.REFERENCE_RETRIEVED,
            requestId = sendInRequest.requestId.parseOrGenerateUuid(),
            messageId = sendInRequest.messageId,
            eventData = eventData,
            conversationId = sendInRequest.conversationId
        )
    }
}

/**
 * Current forwarding rules:
 *  Forward nav:70079 to WS endpoint due to external bug
 *  Forward every other CPA to REST endpoint
 */
private fun SendInRequest.sendToRESTFrikortEndpoint(): Boolean = cpaId != "nav:70079"
