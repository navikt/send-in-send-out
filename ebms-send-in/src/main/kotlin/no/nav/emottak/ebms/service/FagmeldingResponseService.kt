package no.nav.emottak.ebms.service

import no.nav.emottak.fellesformat.FellesFormatXmlMarshaller
import no.nav.emottak.util.LogLevel
import no.nav.emottak.util.asJson
import no.nav.emottak.utils.common.model.Addressing
import no.nav.emottak.utils.common.model.Party
import no.nav.emottak.utils.common.model.PartyId
import no.nav.emottak.utils.common.model.SendInResponse
import no.nav.emottak.utils.environment.getEnvVar
import no.trygdeetaten.xml.eiff._1.EIFellesformat
import org.slf4j.LoggerFactory
import kotlin.uuid.Uuid

object FagmeldingResponseService {

    private val log = LoggerFactory.getLogger("no.nav.emottak.ebms.service.FagmeldingResponseService")

    val navHerId = getEnvVar("NAV_HER_ID", "00000")
    val HER_ID_TYPE = "HER"
    val ORGNR_ID_TYPE = "orgnummer"
    val UKJENT_ID_TYPE = "Ukjent"

    // Mottakenhetblokken kommer fra den opprinnelige requestmeldingen, så ID-feltene refererer til avsender av requesten (samhandler),
    // som jo er mottaker av respons-meldingen.
    private fun getToPartyId(mottakenhetBlokk: EIFellesformat.MottakenhetBlokk): PartyId {
        if (notEmpty(mottakenhetBlokk.herIdentifikator)) {
            return PartyId(HER_ID_TYPE, mottakenhetBlokk.herIdentifikator)
        }
        if (notEmpty(mottakenhetBlokk.orgNummer)) {
            return PartyId(ORGNR_ID_TYPE, mottakenhetBlokk.orgNummer)
        }
        return PartyId(UKJENT_ID_TYPE, mottakenhetBlokk.avsender)
    }

    // har ikke to-role, den må legges på av ebms-async
    fun getResponse(fellesFormatResponse: EIFellesformat): SendInResponse {
        val toPartyId = getToPartyId(fellesFormatResponse.mottakenhetBlokk)

        val response = SendInResponse(
            messageId = Uuid.random().toString(),
            refToMessageId = fellesFormatResponse.mottakenhetBlokk.ediLoggId,
            conversationId = fellesFormatResponse.mottakenhetBlokk.ebXMLSamtaleId,
            cpaId = fellesFormatResponse.mottakenhetBlokk.partnerReferanse,
            addressing = Addressing(
                Party(listOf(toPartyId), ""),
                Party(listOf(PartyId(HER_ID_TYPE, navHerId)), fellesFormatResponse.mottakenhetBlokk.ebRole),
                fellesFormatResponse.mottakenhetBlokk.ebService,
                fellesFormatResponse.mottakenhetBlokk.ebAction
            ),
            payload = FellesFormatXmlMarshaller.marshalToByteArray(
                fellesFormatResponse.msgHead ?: fellesFormatResponse.appRec
            ),
            requestId = Uuid.random().toString()
        )
        log.asJson(
            LogLevel.DEBUG,
            "Sending SendInResponse",
            response,
            SendInResponse.serializer()
        )
        return makeHackForPossiblyConfusedRoleAndAction(response)
    }

    private fun notEmpty(s: String?): Boolean {
        return s != null && s.isNotBlank()
    }

    /* Denne er for å håndtere svar fra Trekkopplysning sitt fagsystem, som ser ut til å kunne bytte om ebAction og ebRole.
       Ser ut som role alltid skal være Ytelsesutbetaler, og action enten Kvittering eller Avvisning.
       Satser på at det er tilstrekkelig å fange opp om en av disse 3 er feilplassert */
    private fun makeHackForPossiblyConfusedRoleAndAction(response: SendInResponse): SendInResponse {
        if (response.addressing.from.role in listOf("Kvittering", "Avvisning") || response.addressing.action == "Ytelsesutbetaler") {
            log.warn("Performs hack to exchange role and action values")

            val newResponse = SendInResponse(
                messageId = response.messageId,
                refToMessageId = response.refToMessageId,
                conversationId = response.conversationId,
                cpaId = response.cpaId,
                addressing = Addressing(
                    response.addressing.to,
                    Party(response.addressing.from.partyId, response.addressing.action),
                    response.addressing.service,
                    response.addressing.from.role
                ),
                payload = response.payload,
                requestId = response.requestId
            )
            log.asJson(
                LogLevel.DEBUG,
                "Sending CORRECTED SendInResponse",
                newResponse,
                SendInResponse.serializer()
            )
            return newResponse
        }
        return response
    }
}
