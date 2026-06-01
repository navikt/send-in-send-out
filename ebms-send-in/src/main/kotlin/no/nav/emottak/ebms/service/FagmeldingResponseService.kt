package no.nav.emottak.ebms.service

import no.nav.emottak.ebms.utils.SupportedAsyncServiceType
import no.nav.emottak.ebms.utils.SupportedAsyncServiceType.Companion.toSupportedAsyncService
import no.nav.emottak.fellesformat.FellesFormatXmlMarshaller
import no.nav.emottak.fellesformat.NYE_EMOTTAK_ID_PREFIX
import no.nav.emottak.trekkopplysning.apprecTrekkopplysningMarshaller
import no.nav.emottak.trekkopplysning.msgheadTrekkopplysningMarshaller
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
    val ENH_ID_TYPE = "ENH"
    val UKJENT_ID_TYPE = "Ukjent"

    // Mottakenhetblokken kommer fra den opprinnelige requestmeldingen, så ID-feltene refererer til avsender av requesten (samhandler),
    // som jo er mottaker av respons-meldingen.
    private fun getToPartyId(mottakenhetBlokk: EIFellesformat.MottakenhetBlokk): List<PartyId> {
        return mutableListOf<PartyId>().apply {
            if (notEmpty(mottakenhetBlokk.herIdentifikator)) {
                add(PartyId(HER_ID_TYPE, mottakenhetBlokk.herIdentifikator))
            }
            if (notEmpty(mottakenhetBlokk.orgNummer)) {
                add(PartyId(ORGNR_ID_TYPE, mottakenhetBlokk.orgNummer))
                add(PartyId(ENH_ID_TYPE, mottakenhetBlokk.orgNummer))
            }
        }
    }

    fun getResponse(fellesFormatResponse: EIFellesformat): SendInResponse {
        val toPartyIds = getToPartyId(fellesFormatResponse.mottakenhetBlokk)
        val toRole =
            when (fellesFormatResponse.mottakenhetBlokk.ebService.toSupportedAsyncService()) {
                SupportedAsyncServiceType.Trekkopplysning ->
                    "Fordringshaver"
                SupportedAsyncServiceType.Sykmelding ->
                    "Sykmelder"
                SupportedAsyncServiceType.Legemelding ->
                    "Lege"
                SupportedAsyncServiceType.Unsupported ->
                    throw NotImplementedError(
                        "Service: ${fellesFormatResponse.mottakenhetBlokk.ebService} is not implemented"
                    )
            }

        val xmlMarshaller = when (fellesFormatResponse.mottakenhetBlokk.ebService.toSupportedAsyncService()) {
            SupportedAsyncServiceType.Trekkopplysning ->
                if (fellesFormatResponse.msgHead != null) {
                    msgheadTrekkopplysningMarshaller
                } else {
                    apprecTrekkopplysningMarshaller
                }
            SupportedAsyncServiceType.Unsupported -> FellesFormatXmlMarshaller
        }

        // todo foreløpig løsning
        var refToMessageId = fellesFormatResponse.mottakenhetBlokk.ediLoggId
        if (fellesFormatResponse.mottakenhetBlokk.ebService.toSupportedAsyncService() == SupportedAsyncServiceType.Legemelding) {
            refToMessageId = refToMessageId.removePrefix(NYE_EMOTTAK_ID_PREFIX)
        }
        val response = SendInResponse(
            messageId = Uuid.random().toString(),
            refToMessageId = refToMessageId,
            conversationId = fellesFormatResponse.mottakenhetBlokk.ebXMLSamtaleId ?: "",
            cpaId = fellesFormatResponse.mottakenhetBlokk.partnerReferanse ?: "",
            addressing = Addressing(
                Party(toPartyIds, toRole),
                Party(listOf(PartyId(HER_ID_TYPE, navHerId)), fellesFormatResponse.mottakenhetBlokk.ebRole ?: ""),
                fellesFormatResponse.mottakenhetBlokk.ebService,
                fellesFormatResponse.mottakenhetBlokk.ebAction
            ),
            payload = xmlMarshaller.marshalToByteArray(
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
        return response
    }

    private fun notEmpty(s: String?): Boolean = !s.isNullOrBlank()
}
