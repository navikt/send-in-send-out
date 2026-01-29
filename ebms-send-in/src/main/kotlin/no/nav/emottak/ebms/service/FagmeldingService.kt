package no.nav.emottak.ebms.service

import arrow.core.Either
import arrow.core.raise.Raise
import arrow.core.raise.either
import io.micrometer.core.instrument.MeterRegistry
import kotlinx.serialization.json.Json
import no.nav.emottak.config.Configurator.config
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
import no.trygdeetaten.xml.eiff._1.EIFellesformat
import org.slf4j.LoggerFactory
import kotlin.uuid.Uuid

object FagmeldingService {
    private val log = LoggerFactory.getLogger("no.nav.emottak.ebms.service.FagmeldingService")

    suspend fun processRequest(
        sendInRequest: SendInRequest,
        meterRegistry: MeterRegistry,
        eventRegistrationService: EventRegistrationService
    ): Either<Throwable, SendInResponse> = either {
        when (sendInRequest.addressing.service.toSupportedService()) {
            SupportedServiceType.Inntektsforesporsel ->
                timed(meterRegistry, "Inntektsforesporsel") {
                    getInntektsforesporsel(sendInRequest, eventRegistrationService)
                }

            SupportedServiceType.HarBorgerEgenandelFritak ->
                when (sendInRequest.sendToRESTFrikortEndpoint()) {
                    true -> getHarBorgerEgenandelFritakREST(sendInRequest, eventRegistrationService)
                    false -> {
                        timed(meterRegistry, "frikort-sporing") {
                            getHarBorgerFrikort(sendInRequest, eventRegistrationService)
                        }
                    }
                }

            SupportedServiceType.HarBorgerFrikort ->
                when (sendInRequest.sendToRESTFrikortEndpoint()) {
                    true -> getHarBorgerFrikortREST(sendInRequest, eventRegistrationService)
                    false -> {
                        timed(meterRegistry, "frikort-sporing") {
                            getHarBorgerFrikort(sendInRequest, eventRegistrationService)
                        }
                    }
                }

            SupportedServiceType.HarBorgerFrikortMengde ->
                timed(meterRegistry, "frikortMengde-sporing") {
                    getHarBorgerFrikortMengde(sendInRequest, eventRegistrationService)
                }

            SupportedServiceType.PasientlisteForesporsel ->
                timed(meterRegistry, "PasientlisteForesporsel") {
                    getPasientlisteForesporsel(sendInRequest, eventRegistrationService)
                }

            SupportedServiceType.Unsupported ->
                throw NotImplementedError(
                    "Service: ${sendInRequest.addressing.service} is not implemented"
                )
        }.also {
            eventRegistrationService.registerEventMessageDetails(sendInRequest, it)
        }.also {
            eventRegistrationService.registerEvent(
                EventType.MESSAGE_RECEIVED_FROM_FAGSYSTEM,
                it.requestId.parseOrGenerateUuid(),
                ""
            )
        }
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
            extractReferenceParameter(sendInRequest, this, eventRegistrationService)
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
            extractReferenceParameter(sendInRequest, this, eventRegistrationService)
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
        sendInRequest: SendInRequest,
        eventRegistrationService: EventRegistrationService
    ): SendInResponse = Either.catch {
        log.info("Message forwarded to HarBorgerFrikort REST")
        with(sendInRequest.asEIFellesFormatWithFrikort()) {
            extractReferenceParameter(sendInRequest, this, eventRegistrationService)
            postHarBorgerFrikort(this.toFrikortsporringRequest()).also {
                eventRegistrationService.registerEvent(
                    EventType.MESSAGE_SENT_TO_FAGSYSTEM,
                    sendInRequest.requestId.parseOrGenerateUuid(),
                    sendInRequest.messageId
                )
            }
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
        sendInRequest: SendInRequest,
        eventRegistrationService: EventRegistrationService
    ): SendInResponse = Either.catch {
        log.info("Message forwarded to HarBorgerEgenandelFritak REST")
        with(sendInRequest.asEIFellesFormatWithFrikort()) {
            extractReferenceParameter(sendInRequest, this, eventRegistrationService)
            postHarBorgerEgenandelfritak(this.toFrikortsporringRequest()).also {
                eventRegistrationService.registerEvent(
                    EventType.MESSAGE_SENT_TO_FAGSYSTEM,
                    sendInRequest.requestId.parseOrGenerateUuid(),
                    sendInRequest.messageId
                )
            }
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
        sendInRequest: SendInRequest,
        eventRegistrationService: EventRegistrationService
    ): SendInResponse = Either.catch {
        with(sendInRequest.asEIFellesFormat()) {
            extractReferenceParameter(sendInRequest, this, eventRegistrationService)
            frikortsporring(this).also {
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
            sendInRequest.requestId.parseOrGenerateUuid(),
            sendInRequest.messageId,
            eventData
        )
    }
}

private fun SendInRequest.sendToRESTFrikortEndpoint() = config().clusterName.isDev() || config().frikortCpalist.contains(this.cpaId)
