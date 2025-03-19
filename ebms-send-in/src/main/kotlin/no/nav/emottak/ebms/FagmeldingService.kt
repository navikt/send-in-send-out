package no.nav.emottak.ebms

import arrow.core.Either
import arrow.core.raise.either
import io.micrometer.core.instrument.MeterRegistry
import no.kith.xmlstds.msghead._2006_05_24.MsgHead
import no.nav.emottak.ebms.SupportedService.Companion.toSupportedService
import no.nav.emottak.ebms.utils.timed
import no.nav.emottak.fellesformat.FellesFormatXmlMarshaller
import no.nav.emottak.fellesformat.asEIFellesFormat
import no.nav.emottak.frikort.frikortsporring
import no.nav.emottak.frikort.frikortsporringMengde
import no.nav.emottak.frikort.unmarshal
import no.nav.emottak.melding.model.SendInRequest
import no.nav.emottak.melding.model.SendInResponse
import no.nav.emottak.pasientliste.PasientlisteService
import no.nav.emottak.utbetaling.UtbetalingClient
import no.nav.emottak.utbetaling.UtbetalingXmlMarshaller
import no.nav.emottak.util.LogLevel
import no.nav.emottak.util.asJson
import no.nav.emottak.util.asXml
import no.nav.emottak.util.birthDay
import no.nav.emottak.util.marker
import no.nav.emottak.util.refParam
import no.nav.emottak.util.refParamFrikort
import no.nav.emottak.utils.isProdEnv
import org.slf4j.LoggerFactory
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

object FagmeldingService {
    private val log = LoggerFactory.getLogger("no.nav.emottak.ebms.FagmeldingService")

    @OptIn(ExperimentalUuidApi::class)
    fun processRequest(
        sendInRequest: SendInRequest,
        meterRegistry: MeterRegistry
    ): Either<Throwable, SendInResponse> = either {
        when (sendInRequest.addressing.service.toSupportedService()) {
            SupportedService.Inntektsforesporsel ->
                timed(meterRegistry, "Inntektsforesporsel") {
                    Either.catch {
                        UtbetalingClient.behandleInntektsforesporsel(
                            sendInRequest.messageId,
                            sendInRequest.conversationId,
                            sendInRequest.payload
                        )
                    }.bind().let { msgHeadResponse ->
                        SendInResponse(
                            messageId = sendInRequest.messageId,
                            conversationId = sendInRequest.conversationId,
                            addressing = sendInRequest.addressing.replyTo(
                                sendInRequest.addressing.service,
                                msgHeadResponse.msgInfo.type.v
                            ),
                            payload = UtbetalingXmlMarshaller.marshalToByteArray(msgHeadResponse),
                            requestId = Uuid.random().toString()
                        )
                    }.also {
                        val refParam = refParam(sendInRequest.asEIFellesFormat())
                        log.info(sendInRequest.marker(), "refParam ${birthDay(refParam)}")
                    }
                }

            SupportedService.HarBorgerEgenandelFritak, SupportedService.HarBorgerFrikort ->
                timed(meterRegistry, "frikort-sporing") {
                    with(sendInRequest.asEIFellesFormat()) {
                        frikortsporring(this).let { response ->
                            SendInResponse(
                                messageId = sendInRequest.messageId,
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
                    }.also {
                        val payload = sendInRequest.payload
                        val msgHead = unmarshal(String(payload), MsgHead::class.java)

                        val action = msgHead.document.map { doc -> doc.refDoc.content.any }.first().first()
                        val refParam = refParamFrikort(action)
                        log.info(sendInRequest.marker(), "refParam ${birthDay(refParam)}")
                    }
                }

            SupportedService.HarBorgerFrikortMengde ->
                timed(meterRegistry, "frikortMengde-sporing") {
                    with(sendInRequest.asEIFellesFormat()) {
                        frikortsporringMengde(this).let { response ->
                            SendInResponse(
                                messageId = sendInRequest.messageId,
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
                    }.also {
                        val payload = sendInRequest.payload
                        val msgHead = unmarshal(String(payload), MsgHead::class.java)

                        val action = msgHead.document.map { doc -> doc.refDoc.content.any }.first().first()
                        val refParam = refParamFrikort(action)
                        log.info(sendInRequest.marker(), "refParam $refParam")
                    }
                }

            SupportedService.PasientlisteForesporsel ->
                timed(meterRegistry, "PasientlisteForesporsel") {
                    if (isProdEnv()) {
                        throw NotImplementedError(
                            "PasientlisteForesporsel is used in prod. Feature is not ready. Aborting."
                        )
                    }
                    with(sendInRequest.asEIFellesFormat()) {
                        log.asXml(
                            LogLevel.DEBUG,
                            "Wrapped message (fellesformatRequest)",
                            this,
                            sendInRequest.marker()
                        )
                        PasientlisteService.pasientlisteForesporsel(this).let { fellesformatResponse ->
                            SendInResponse(
                                messageId = sendInRequest.messageId,
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
                                val refParam = refParam(sendInRequest.asEIFellesFormat())
                                log.info(sendInRequest.marker(), "refParam ${birthDay(refParam)}")
                                log.asJson(
                                    LogLevel.DEBUG,
                                    "Sending SendInResponse",
                                    it,
                                    SendInResponse.serializer(),
                                    sendInRequest.marker()
                                )
                            }
                        }
                    }
                }

            SupportedService.Unsupported ->
                throw NotImplementedError(
                    "Service: ${sendInRequest.addressing.service} is not implemented"
                )
        }
    }
}
