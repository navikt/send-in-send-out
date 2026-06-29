package no.nav.emottak.fellesformat

import no.kith.xmlstds.msghead._2006_05_24.MsgHead
import no.nav.emottak.NYE_EMOTTAK_LEGEMELDING_ID_PREFIX
import no.nav.emottak.frikort.frikortSporringXmlMarshaller
import no.nav.emottak.util.toXmlGregorianCalendar
import no.nav.emottak.utils.common.model.PartyId
import no.nav.emottak.utils.common.model.SendInRequest
import no.trygdeetaten.xml.eiff._1.EIFellesformat
import no.trygdeetaten.xml.eiff._1.ObjectFactory
import java.time.Instant

private val fellesFormatFactory = ObjectFactory()

val UKJENT_ID = "Ukjent"

fun SendInRequest.asEIFellesFormat(): EIFellesformat =
    fellesFormatFactory.createEIFellesformat().apply {
        mottakenhetBlokk = createFellesFormatMottakEnhetBlokk(this@asEIFellesFormat)
        msgHead = unmarshal(this@asEIFellesFormat.payload.toString(Charsets.UTF_8), MsgHead::class.java)
    }

fun SendInRequest.asEIFellesFormat_WithoutPayload(): EIFellesformat =
    fellesFormatFactory.createEIFellesformat().apply {
        mottakenhetBlokk = createFellesFormatMottakEnhetBlokk(this@asEIFellesFormat_WithoutPayload)
    }

fun SendInRequest.asEIFellesFormat_Sykmelding(): EIFellesformat =
    fellesFormatFactory.createEIFellesformat().apply {
        mottakenhetBlokk = createFellesFormatMottakEnhetBlokk_Sykmelding(this@asEIFellesFormat_Sykmelding)
    }

fun SendInRequest.asEIFellesFormat_Legemelding(): EIFellesformat =
    fellesFormatFactory.createEIFellesformat().apply {
        mottakenhetBlokk = createFellesFormatMottakEnhetBlokk_Legemelding(this@asEIFellesFormat_Legemelding)
    }

fun SendInRequest.asEIFellesFormat_FrikortMengde(): EIFellesformat =
    fellesFormatFactory.createEIFellesformat().apply {
        mottakenhetBlokk = createFellesFormatMottakEnhetBlokk_Frikort(this@asEIFellesFormat_FrikortMengde)
        msgHead = unmarshal(this@asEIFellesFormat_FrikortMengde.payload.toString(Charsets.UTF_8), MsgHead::class.java)
    }

fun SendInRequest.asEIFellesFormatWithFrikort(): EIFellesformat =
    fellesFormatFactory.createEIFellesformat().apply {
        mottakenhetBlokk = createFellesFormatMottakEnhetBlokk_Frikort(this@asEIFellesFormatWithFrikort)
        msgHead = frikortSporringXmlMarshaller.unmarshal(this@asEIFellesFormatWithFrikort.payload.toString(Charsets.UTF_8), MsgHead::class.java)
    }

private fun createFellesFormatMottakEnhetBlokk(sendInRequest: SendInRequest): EIFellesformat.MottakenhetBlokk {
    return fellesFormatFactory.createEIFellesformatMottakenhetBlokk().apply {
        ebXMLSamtaleId = sendInRequest.conversationId
        ebAction = sendInRequest.addressing.action
        ebService = sendInRequest.addressing.service
        ebRole = sendInRequest.addressing.from.role
        herIdentifikator = sendInRequest.addressing.from.partyId.getIdentifikatorByType("HER")
        orgNummer = sendInRequest.addressing.from.partyId.getIdentifikatorByType("orgnummer", "ENH")
        avsender = sendInRequest.addressing.from.partyId.getIdentifikatorByType("HER", "ENH", "orgnummer")
        mottattDatotid = Instant.now().toXmlGregorianCalendar()
        ediLoggId = sendInRequest.messageId
        meldingsType = "xml"
        this.partnerReferanse = sendInRequest.cpaId
        avsenderRef = ""
        // todo når vi får meldingstyper som skal være signert av org må vi velge å sette avsenderOrgNrFraDigSignatur
        if (sendInRequest.signedOf != null) {
            avsenderFnrFraDigSignatur = sendInRequest.signedOf
        }
    }
}

// Disse har vært i frikortspørringene, usikker på om de er nødvendige
private fun createFellesFormatMottakEnhetBlokk_Frikort(sendInRequest: SendInRequest): EIFellesformat.MottakenhetBlokk {
    return createFellesFormatMottakEnhetBlokk(sendInRequest).apply {
        mottaksId = sendInRequest.messageId
        avsenderFnrFraDigSignatur = sendInRequest.signedOf ?: "NA"
        avsenderOrgNrFraDigSignatur = ""
    }
}

// Kan fjernes når vi tar inn fnr uansett ?
private fun createFellesFormatMottakEnhetBlokk_Sykmelding(sendInRequest: SendInRequest): EIFellesformat.MottakenhetBlokk {
    return createFellesFormatMottakEnhetBlokk(sendInRequest).apply {
        avsenderFnrFraDigSignatur = sendInRequest.signedOf ?: "NA"
    }
}

// MIDLERTIDIG: Trenger et prefix for å skille mellom Legemeldinger sendt via gamle/nye eMottak, for å kunne rute responsen riktig
// Når det ikke sendes Legemeldinger gjennom gamle eMottak lenger, kan prefikset fjernes
private fun createFellesFormatMottakEnhetBlokk_Legemelding(sendInRequest: SendInRequest): EIFellesformat.MottakenhetBlokk {
    return createFellesFormatMottakEnhetBlokk(sendInRequest).apply {
        avsenderFnrFraDigSignatur = sendInRequest.signedOf ?: "NA"
        ediLoggId = NYE_EMOTTAK_LEGEMELDING_ID_PREFIX + sendInRequest.messageId
    }
}

private fun List<PartyId>.getIdentifikatorByType(vararg types: String) =
    this.firstOrNull { types.contains(it.type) }?.value ?: ""
