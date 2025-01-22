package no.nav.emottak.pasientliste

import no.nav.emottak.fellesformat.FellesFormatXmlMarshaller
import no.nav.emottak.fellesformat.wrapMessageInEIFellesFormat
import no.nav.emottak.melding.model.SendInRequest
import no.nav.emottak.melding.model.SendInResponse
import no.nav.emottak.util.asJson
import no.nav.emottak.util.asXml
import org.slf4j.Logger
import org.slf4j.LoggerFactory

object PasientlisteService {

    const val CONFLICT_SIGNING_SSN = "Sender FNR og legen som har signert meldingen skall ikke vare forskjelige."
    private val log: Logger = LoggerFactory.getLogger("PasientlisteService")

    fun pasientlisteForesporsel(request: SendInRequest): SendInResponse {
        return when (request.addressing.action) {
            "HentPasientliste", "StartAbonnement", "StoppAbonnement", "HentAbonnementStatus" -> forwardRequest(request)
            else -> throw NotImplementedError("Action: ${request.addressing.action} for service: ${request.addressing.service} is not implemented")
        }
    }

    private fun forwardRequest(request: SendInRequest): SendInResponse {
        log.asJson(
            message = "Received SendInRequest",
            obj = request,
            serializer = SendInRequest.serializer()
        )

        val fellesformatRequest = wrapMessageInEIFellesFormat(request)
        log.asXml(
            message = "Wrapped message (fellesformatRequest)",
            obj = fellesformatRequest
        )

        val senderFnr = fellesformatRequest.msgHead.msgInfo.sender.organisation.healthcareProfessional.ident.first().id
        if (senderFnr != request.signedOf) {
            throw RuntimeException(CONFLICT_SIGNING_SSN)
        }
        val fellesformatResponse = PasientlisteClient.sendRequest(fellesformatRequest)
        log.asXml(
            message = "Response from PasientlisteClient",
            obj = fellesformatResponse
        )

        val sendInResponse = SendInResponse(
            request.messageId,
            request.conversationId,
            request.addressing.replyTo(
                fellesformatResponse.mottakenhetBlokk.ebService,
                fellesformatResponse.mottakenhetBlokk.ebAction
            ),
            FellesFormatXmlMarshaller.marshalToByteArray(fellesformatResponse.appRec)
        )
        log.asJson(
            message = "Sending SendInResponse",
            obj = sendInResponse,
            serializer = SendInResponse.serializer()
        )
        return sendInResponse
    }
}
