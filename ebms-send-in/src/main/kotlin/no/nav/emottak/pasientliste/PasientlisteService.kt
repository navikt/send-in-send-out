package no.nav.emottak.pasientliste

import no.nav.emottak.fellesformat.FellesFormatXmlMarshaller
import no.nav.emottak.fellesformat.wrapMessageInEIFellesFormat
import no.nav.emottak.melding.model.SendInRequest
import no.nav.emottak.melding.model.SendInResponse
import no.nav.emottak.pasientliste.validator.PasientlisteValidator
import no.nav.emottak.util.LogLevel
import no.nav.emottak.util.asJson
import no.nav.emottak.util.asXml
import no.nav.emottak.util.marker
import org.slf4j.Logger
import org.slf4j.LoggerFactory

object PasientlisteService {

    private val log: Logger = LoggerFactory.getLogger(PasientlisteService::class.java)

    fun pasientlisteForesporsel(request: SendInRequest, responseRequestId: String): SendInResponse {
        return when (request.addressing.action) {
            "HentPasientliste", "StartAbonnement", "StoppAbonnement", "HentAbonnementStatus" -> forwardRequest(request, responseRequestId)
            else -> throw NotImplementedError("Action: ${request.addressing.action} for service: ${request.addressing.service} is not implemented")
        }
    }

    private fun forwardRequest(request: SendInRequest, responseRequestId: String): SendInResponse {
        log.asJson(LogLevel.DEBUG, "Received SendInRequest", request, SendInRequest.serializer(), request.marker())

        val fellesformatRequest = wrapMessageInEIFellesFormat(request)
        log.asXml(LogLevel.DEBUG, "Wrapped message (fellesformatRequest)", fellesformatRequest, request.marker())

        PasientlisteValidator.validateLegeIsAlsoSigner(fellesformatRequest, request.signedOf!!)

        val fellesformatResponse = PasientlisteClient.sendRequest(fellesformatRequest)
        log.asXml(LogLevel.DEBUG, "Response from PasientlisteClient", fellesformatResponse, request.marker())

        val sendInResponse = SendInResponse(
            request.messageId,
            request.conversationId,
            request.addressing.replyTo(
                fellesformatResponse.mottakenhetBlokk.ebService,
                fellesformatResponse.mottakenhetBlokk.ebAction
            ),
            FellesFormatXmlMarshaller.marshalToByteArray(fellesformatResponse.appRec),
            responseRequestId
        )
        log.asJson(LogLevel.DEBUG, "Sending SendInResponse", sendInResponse, SendInResponse.serializer(), request.marker())

        return sendInResponse
    }
}
