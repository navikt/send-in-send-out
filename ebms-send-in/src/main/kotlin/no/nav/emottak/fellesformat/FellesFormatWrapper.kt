package no.nav.emottak.fellesformat

import no.kith.xmlstds.msghead._2006_05_24.MsgHead
import no.nav.emottak.constants.LogIndex.DOCUMENT_EGENANDELFRIKORT_FNUMMER
import no.nav.emottak.constants.LogIndex.DOCUMENT_HARBORGERFRIKORT_SERVICE
import no.nav.emottak.constants.LogIndex.DOCUMENT_INNTEKTFORESPORSEL_FNUMMER
import no.nav.emottak.constants.LogIndex.DOCUMENT_INNTEKTFORESPORSEL_SERVICE
import no.nav.emottak.constants.LogIndex.DOCUMENT_PASIENTLISTEFORESPORSEL_FNUMMER
import no.nav.emottak.constants.LogIndex.DOCUMENT_PASIENTLISTEFORESPORSEL_SERVICE
import no.nav.emottak.ebms.log
import no.nav.emottak.melding.model.Addressing
import no.nav.emottak.melding.model.Party
import no.nav.emottak.melding.model.PartyId
import no.nav.emottak.melding.model.SendInRequest
import no.nav.emottak.util.birthDay
import no.nav.emottak.util.createDocument
import no.nav.emottak.util.getEnvVar
import no.nav.emottak.util.marker
import no.nav.emottak.util.refParam
import no.nav.emottak.util.toXMLGregorianCalendar
import no.trygdeetaten.xml.eiff._1.EIFellesformat
import no.trygdeetaten.xml.eiff._1.ObjectFactory
import java.io.ByteArrayInputStream
import java.time.Instant

private val fellesFormatFactory = ObjectFactory()

fun EIFellesformat.addressing(toParty: Party): Addressing {
    val sender = this.msgHead.msgInfo.sender
    val reciever = this.msgHead.msgInfo.receiver
    val fromList = sender.organisation.ident.map { PartyId(it.typeId.v, it.id) }.toList()
    val partyFrom = Party(fromList, this.mottakenhetBlokk.ebRole)
    val toList = reciever.organisation.ident.map { PartyId(it.typeId.v, it.id) }.toList()
    val partyTo = Party(toList, toParty.role)
    return Addressing(partyTo, partyFrom, this.mottakenhetBlokk.ebService, this.mottakenhetBlokk.ebAction)
}

fun wrapMessageInEIFellesFormat(sendInRequest: SendInRequest): EIFellesformat =
    fellesFormatFactory.createEIFellesformat().also {
        it.mottakenhetBlokk = createFellesFormatMottakEnhetBlokk(sendInRequest)
        it.msgHead = unmarshal(sendInRequest.payload.toString(Charsets.UTF_8), MsgHead::class.java)
    }.also {
        val document = createDocument(ByteArrayInputStream(FellesFormatXmlMarshaller.marshal(it).toByteArray()))
        val fnr = when (it.mottakenhetBlokk.ebService) {
            DOCUMENT_HARBORGERFRIKORT_SERVICE -> refParam(document.getChildNodes(), DOCUMENT_EGENANDELFRIKORT_FNUMMER)
            DOCUMENT_PASIENTLISTEFORESPORSEL_SERVICE -> refParam(document.getChildNodes(), DOCUMENT_PASIENTLISTEFORESPORSEL_FNUMMER)
            DOCUMENT_INNTEKTFORESPORSEL_SERVICE -> refParam(document.getChildNodes(), DOCUMENT_INNTEKTFORESPORSEL_FNUMMER)
            else -> "NA"
        }
        log.info(sendInRequest.marker(mapOf("refParam" to birthDay(fnr))), "Melding sendt til fagsystem")
        if (getEnvVar("NAIS_CLUSTER_NAME", "local") != "prod-fss") {
            log.info("Sending in request to fag with body " + FellesFormatXmlMarshaller.marshal(it))
        }
    }

private fun createFellesFormatMottakEnhetBlokk(sendInRequest: SendInRequest): EIFellesformat.MottakenhetBlokk =
    fellesFormatFactory.createEIFellesformatMottakenhetBlokk().also {
        it.ebXMLSamtaleId = sendInRequest.conversationId
        it.ebAction = sendInRequest.addressing.action
        it.ebService = sendInRequest.addressing.service
        it.ebRole = sendInRequest.addressing.from.role
        it.avsender = "TODO1" // Hentes fra from. Usikker på hvilket felt siden det kan være flere.
        it.avsenderRef = "TODO2" // Hentet fra cert: Eksempelverdi: "SERIALNUMBER=132547698, CN=Blå &amp; Bjørnebær AS, O=Blå &amp; Bjørnebær AS, C=NO"
        it.mottaksId = sendInRequest.messageId
        it.mottattDatotid = Instant.now().toXMLGregorianCalendar()
        it.ediLoggId = sendInRequest.messageId
        it.avsenderFnrFraDigSignatur = "TODO3"
        it.avsenderOrgNrFraDigSignatur = "TODO4"
        it.herIdentifikator = "TODO5" // Avsender HER ID?
        it.orgNummer = "TODO6" // Avsender?
        it.meldingsType = "xml"
        it.partnerReferanse = sendInRequest.cpaId
    }
