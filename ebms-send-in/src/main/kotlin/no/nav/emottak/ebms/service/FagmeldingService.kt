package no.nav.emottak.ebms.service

import arrow.core.Either
import arrow.core.raise.Raise
import arrow.core.raise.either
import io.micrometer.core.instrument.MeterRegistry
import kotlinx.serialization.json.Json
import no.nav.emottak.ebms.MqService
import no.nav.emottak.ebms.MqServiceMapper
import no.nav.emottak.ebms.utils.AsyncRoutingAction.Companion.toAsyncRoutingAction
import no.nav.emottak.ebms.utils.SupportedAsyncServiceType
import no.nav.emottak.ebms.utils.SupportedAsyncServiceType.Companion.toSupportedAsyncService
import no.nav.emottak.ebms.utils.SupportedSyncServiceType
import no.nav.emottak.ebms.utils.SupportedSyncServiceType.Companion.toSupportedService
import no.nav.emottak.ebms.utils.timed
import no.nav.emottak.fellesformat.FellesFormatXmlMarshaller
import no.nav.emottak.fellesformat.asEIFellesFormat
import no.nav.emottak.fellesformat.asEIFellesFormatWithFrikort
import no.nav.emottak.fellesformat.asEIFellesFormat_FrikortMengde
import no.nav.emottak.fellesformat.asEIFellesFormat_Legemelding
import no.nav.emottak.fellesformat.asEIFellesFormat_Sykmelding
import no.nav.emottak.fellesformat.asEIFellesFormat_WithoutPayload
import no.nav.emottak.frikort.frikortsporringMengde
import no.nav.emottak.frikort.getMinimalContentXmlMarshaller
import no.nav.emottak.frikort.rest.postHarBorgerEgenandelfritak
import no.nav.emottak.frikort.rest.postHarBorgerFrikort
import no.nav.emottak.frikort.rest.toFrikortsporringRequest
import no.nav.emottak.frikort.rest.toMsgHead
import no.nav.emottak.utbetaling.UtbetalingClient
import no.nav.emottak.utbetaling.UtbetalingXmlMarshaller
import no.nav.emottak.util.EventRegistrationService
import no.nav.emottak.util.extractReferenceParameter
import no.nav.emottak.utils.common.model.SendInRequest
import no.nav.emottak.utils.common.model.SendInResponse
import no.nav.emottak.utils.common.parseOrGenerateUuid
import no.nav.emottak.utils.kafka.model.EventDataType
import no.nav.emottak.utils.kafka.model.EventType
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
            SupportedSyncServiceType.Inntektsforesporsel ->
                timed(meterRegistry, "Inntektsforesporsel") {
                    log.debug("Inntektsforesporsel is processed synchronously")
                    getInntektsforesporsel(sendInRequest, eventRegistrationService).also {
                        persistEventsAndMessageDetails(eventRegistrationService, sendInRequest, it)
                    }
                }

            SupportedSyncServiceType.HarBorgerEgenandelFritak -> {
                log.debug("HarBorgerEgenandelFritak is processed synchronously via REST")
                getHarBorgerEgenandelFritakREST(sendInRequest)
            }

            SupportedSyncServiceType.HarBorgerFrikort -> {
                log.debug("HarBorgerFrikort is processed synchronously via REST")
                getHarBorgerFrikortREST(sendInRequest)
            }

            SupportedSyncServiceType.HarBorgerFrikortMengde ->
                timed(meterRegistry, "frikortMengde-sporing") {
                    log.debug("HarBorgerFrikortMengde is processed synchronously")
                    getHarBorgerFrikortMengde(sendInRequest, eventRegistrationService).also {
                        persistEventsAndMessageDetails(eventRegistrationService, sendInRequest, it)
                    }
                }

            SupportedSyncServiceType.Unsupported ->
                throw NotImplementedError(
                    "Service: ${sendInRequest.addressing.service} is not implemented"
                )
        }
    }

    private fun persistEventsAndMessageDetails(eventRegistrationService: EventRegistrationService, sendInRequest: SendInRequest, sendInResponse: SendInResponse) {
        eventRegistrationService.registerEventMessageDetails(sendInResponse)
        eventRegistrationService.registerEvent(
            EventType.MESSAGE_RECEIVED_FROM_FAGSYSTEM,
            requestId = sendInResponse.requestId.parseOrGenerateUuid(),
            messageId = "",
            conversationId = sendInResponse.conversationId
        )
    }

    suspend fun processRequestAsynchronously(
        sendInRequest: SendInRequest,
        meterRegistry: MeterRegistry,
        eventRegistrationService: EventRegistrationService,
        mqServiceMapper: MqServiceMapper
    ): Either<Throwable, Unit> = either {
        val serviceType = sendInRequest.addressing.service.toSupportedAsyncService()
        when (serviceType) {
            SupportedAsyncServiceType.Trekkopplysning ->
                timed(meterRegistry, "Trekkopplysning") {
                    log.info("Trekkopplysning is processed asynchronously")
                    sendTrekkopplysning(sendInRequest, eventRegistrationService, mqServiceMapper.get(serviceType)!!)
                }
            SupportedAsyncServiceType.Sykmelding ->
                timed(meterRegistry, "Sykmelding") {
                    log.info("Sykmelding is processed asynchronously")
                    sendSykmelding(sendInRequest, eventRegistrationService, mqServiceMapper.get(serviceType)!!)
                }
            SupportedAsyncServiceType.Legemelding ->
                timed(meterRegistry, "Legemelding") {
                    log.info("Legemelding is processed asynchronously")
                    sendLegemelding(sendInRequest, eventRegistrationService, mqServiceMapper.get(serviceType)!!)
                }
            SupportedAsyncServiceType.BehandlerKrav, SupportedAsyncServiceType.HenvendelseFraLege, SupportedAsyncServiceType.HenvendelseFraSaksbehandler,
            SupportedAsyncServiceType.Oppfolgingsplan ->
                timed(meterRegistry, serviceType.service) {
                    log.info(serviceType.service + " is processed asynchronously")
                    sendMessageViaMQ(sendInRequest, eventRegistrationService, mqServiceMapper.get(serviceType)!!)
                }

            // For disse er action viktig for å bestemme hvilken MQ det skal rutes til
            SupportedAsyncServiceType.OppgjorsKontroll, SupportedAsyncServiceType.DialogmoteInnkalling, SupportedAsyncServiceType.ForesporselFraSaksbehandler ->
                timed(meterRegistry, serviceType.service) {
                    log.info(serviceType.service + " is processed asynchronously")
                    val action = sendInRequest.addressing.action.toAsyncRoutingAction(serviceType)
                    sendMessageViaMQ(sendInRequest, eventRegistrationService, mqServiceMapper.get(action)!!)
                }

            // Se under, alle som har "standard" MottakenhetBlokk-.XML, kan bruke sendMessageViaMQ(),
            // med GeneralServiceUsingMq som parameter, konfigurert med riktig MQ-kø.
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
        with(sendInRequest.asEIFellesFormat_FrikortMengde()) {
            persistReferenceParameter(sendInRequest, this.extractReferenceParameter(), eventRegistrationService)
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
        sendInRequest: SendInRequest
    ): SendInResponse = Either.catch {
        with(sendInRequest.asEIFellesFormatWithFrikort()) {
            log.info("Refparam: ${this.extractReferenceParameter()}")
            postHarBorgerFrikort(this.toFrikortsporringRequest())
        }
    }.bind().let { response ->
        val xmlMarshaller = response.eiFellesformat.msgHead.getMinimalContentXmlMarshaller()
        SendInResponse(
            messageId = Uuid.random().toString(),
            refToMessageId = sendInRequest.messageId,
            conversationId = sendInRequest.conversationId,
            cpaId = sendInRequest.cpaId,
            addressing = sendInRequest.addressing.replyTo(
                response.eiFellesformat.mottakenhetBlokk.ebService!!.value,
                response.eiFellesformat.mottakenhetBlokk.ebAction!!
            ),
            payload = xmlMarshaller.marshalToByteArray(
                response.eiFellesformat.msgHead.toMsgHead()
            ),
            requestId = Uuid.random().toString()
        )
    }

    private suspend fun Raise<Throwable>.getHarBorgerEgenandelFritakREST(
        sendInRequest: SendInRequest
    ): SendInResponse = Either.catch {
        with(sendInRequest.asEIFellesFormatWithFrikort()) {
            log.info("Refparam: ${this.extractReferenceParameter()}")
            postHarBorgerEgenandelfritak(this.toFrikortsporringRequest())
        }
    }.bind().let { response ->
        val xmlMarshaller = response.eiFellesformat.msgHead.getMinimalContentXmlMarshaller()
        SendInResponse(
            messageId = Uuid.random().toString(),
            refToMessageId = sendInRequest.messageId,
            conversationId = sendInRequest.conversationId,
            cpaId = sendInRequest.cpaId,
            addressing = sendInRequest.addressing.replyTo(
                response.eiFellesformat.mottakenhetBlokk.ebService!!.value,
                response.eiFellesformat.mottakenhetBlokk.ebAction!!
            ),
            payload = xmlMarshaller.marshalToByteArray(
                response.eiFellesformat.msgHead.toMsgHead()
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
        trekkopplysningService: MqService
    ) = Either.catch {
        with(sendInRequest.asEIFellesFormat()) {
            trekkopplysningService.buildAndSend(this, sendInRequest.payload).also {
                eventRegistrationService.registerEvent(
                    EventType.MESSAGE_SENT_TO_FAGSYSTEM,
                    sendInRequest.requestId.parseOrGenerateUuid(),
                    sendInRequest.messageId
                )
            }
        }
    }.bind()

    // Hvis du kan bruke generell logikk for å utlede en MottakEnhetBlokk fra requesten (ellers må man ha spesifikk asMottakEnhetBlokk()),
    // og hvis du kan bruke "normal" XML med attributter sortert alfabetisk
    private fun Raise<Throwable>.sendMessageViaMQ(
        sendInRequest: SendInRequest,
        eventRegistrationService: EventRegistrationService,
        mqService: MqService
    ) = Either.catch {
        with(sendInRequest.asEIFellesFormat_WithoutPayload()) {
            mqService.buildAndSend(this, sendInRequest.payload).also {
                eventRegistrationService.registerEvent(
                    EventType.MESSAGE_SENT_TO_FAGSYSTEM,
                    sendInRequest.requestId.parseOrGenerateUuid(),
                    sendInRequest.messageId
                )
            }
        }
    }.bind()

    private fun Raise<Throwable>.sendSykmelding(
        sendInRequest: SendInRequest,
        eventRegistrationService: EventRegistrationService,
        syfoMeldingService: MqService
    ) = Either.catch {
        with(sendInRequest.asEIFellesFormat_Sykmelding()) {
            syfoMeldingService.buildAndSend(this, sendInRequest.payload).also {
                eventRegistrationService.registerEvent(
                    EventType.MESSAGE_SENT_TO_FAGSYSTEM,
                    sendInRequest.requestId.parseOrGenerateUuid(),
                    sendInRequest.messageId
                )
            }
        }
    }.bind()

    private fun Raise<Throwable>.sendLegemelding(
        sendInRequest: SendInRequest,
        eventRegistrationService: EventRegistrationService,
        legeMeldingService: MqService
    ) = Either.catch {
        with(sendInRequest.asEIFellesFormat_Legemelding()) {
            legeMeldingService.buildAndSend(this, sendInRequest.payload).also {
                eventRegistrationService.registerEvent(
                    EventType.MESSAGE_SENT_TO_FAGSYSTEM,
                    sendInRequest.requestId.parseOrGenerateUuid(),
                    sendInRequest.messageId
                )
            }
        }
    }.bind()

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
            requestId = sendInRequest.requestId.parseOrGenerateUuid(),
            messageId = sendInRequest.messageId,
            eventData = eventData,
            conversationId = sendInRequest.conversationId
        )
    }
}
