package no.nav.emottak.fellesformat

import no.kith.xmlstds.msghead._2006_05_24.MsgHead
import no.nav.emottak.frikort.frikortSporringXmlMarshaller
import no.nav.emottak.log
import no.nav.emottak.util.toXmlGregorianCalendar
import no.nav.emottak.utils.common.model.PartyId
import no.nav.emottak.utils.common.model.SendInRequest
import no.trygdeetaten.xml.eiff._1.EIFellesformat
import no.trygdeetaten.xml.eiff._1.ObjectFactory
import java.time.Instant

private val fellesFormatFactory = ObjectFactory()

fun SendInRequest.asEIFellesFormat(): EIFellesformat =
    fellesFormatFactory.createEIFellesformat().apply {
        mottakenhetBlokk = createFellesFormatMottakEnhetBlokk(this@asEIFellesFormat)
        msgHead = unmarshal(this@asEIFellesFormat.payload.toString(Charsets.UTF_8), MsgHead::class.java)
    }

fun SendInRequest.asEIFellesFormat_Trekkopplysning(): EIFellesformat =
    fellesFormatFactory.createEIFellesformat().apply {
        mottakenhetBlokk = createFellesFormatMottakEnhetBlokk_Trekkopplysning(this@asEIFellesFormat_Trekkopplysning)
        msgHead = unmarshal(this@asEIFellesFormat_Trekkopplysning.payload.toString(Charsets.UTF_8), MsgHead::class.java)
        val doc = msgHead.document
        if (doc.isEmpty()) {
            log.info("No documents in msgHead")
        } else {
            log.info("Docs: " + doc.size)
            val firstDoc = doc.first()
            val firstRefDoc = firstDoc.refDoc
            val firstContent = firstRefDoc.content.any
            if (firstContent.isEmpty()) {
                log.info("No content in refDoc")
            } else {
                log.info("Content: " + firstContent.size)
                val firstContentAny = firstContent.first()
                log.info("Content: " + firstContentAny.toString())
            }
        }
    }

fun SendInRequest.asEIFellesFormatWithFrikort(): EIFellesformat =
    fellesFormatFactory.createEIFellesformat().apply {
        mottakenhetBlokk = createFellesFormatMottakEnhetBlokk(this@asEIFellesFormatWithFrikort)
        msgHead = frikortSporringXmlMarshaller.unmarshal(this@asEIFellesFormatWithFrikort.payload.toString(Charsets.UTF_8), MsgHead::class.java)
    }

private fun createFellesFormatMottakEnhetBlokk(sendInRequest: SendInRequest): EIFellesformat.MottakenhetBlokk {
    val partnerReferanse = when (sendInRequest.addressing.service) {
        "PasientlisteForesporsel" -> sendInRequest.partnerId?.toString() ?: ""
        else -> sendInRequest.cpaId
    }

    return fellesFormatFactory.createEIFellesformatMottakenhetBlokk().apply {
        ebXMLSamtaleId = sendInRequest.conversationId
        ebAction = sendInRequest.addressing.action
        ebService = sendInRequest.addressing.service
        ebRole = sendInRequest.addressing.from.role
        avsender = "TODO1" // Hentes fra from. Usikker på hvilket felt siden det kan være flere.
        avsenderRef =
            "TODO2" // Hentet fra cert: Eksempelverdi: "SERIALNUMBER=132547698, CN=Blå &amp; Bjørnebær AS, O=Blå &amp; Bjørnebær AS, C=NO"
        mottaksId = sendInRequest.messageId
        mottattDatotid = Instant.now().toXmlGregorianCalendar()
        ediLoggId = sendInRequest.messageId
        avsenderFnrFraDigSignatur = sendInRequest.signedOf ?: "NA"
        avsenderOrgNrFraDigSignatur = "TODO4"
        herIdentifikator = sendInRequest.addressing.from.partyId.getIdentifikatorByType("HER")
        orgNummer = sendInRequest.addressing.from.partyId.getIdentifikatorByType("orgnummer", "ENH")
        meldingsType = "xml"
        this.partnerReferanse = partnerReferanse
    }
}

private fun createFellesFormatMottakEnhetBlokk_Trekkopplysning(sendInRequest: SendInRequest): EIFellesformat.MottakenhetBlokk {
    val partnerReferanse = when (sendInRequest.addressing.service) {
        "PasientlisteForesporsel" -> sendInRequest.partnerId?.toString() ?: ""
        else -> sendInRequest.cpaId
    }

    // Gamle eMottak
    // - sender IKKE avsenderFnrFraDigSignatur, avsenderOrgNrFraDigSignatur, mottaksId, orgNummer. Kommentert ut under.
    // - sender BLANK herIdentifikator todo ikke endret
    // - sender ANNEN verdi for ediLoggId (= mottaksId, a la 2603041315aidn58567.1) todo antar OK
    // - sender ANNEN verdi for partnerReferanse (= partner-ID fra CPA-tabell, a la 21137. Vi får inn CPA-ID her) todo antar OK
    // TODO fiks avsender og -ref.
    //  Gammel putter HER-ID som avsender (mulig ikke alltid?) Let/velg 1
    // ref fra partner_subjectdn i cpa-tabell
    return fellesFormatFactory.createEIFellesformatMottakenhetBlokk().apply {
        ebXMLSamtaleId = sendInRequest.conversationId
        ebAction = sendInRequest.addressing.action
        ebService = sendInRequest.addressing.service
        ebRole = sendInRequest.addressing.from.role
        avsender = "TODO1" // Hentes fra from. Usikker på hvilket felt siden det kan være flere.
        avsenderRef =
            "TODO2" // Hentet fra cert: Eksempelverdi: "SERIALNUMBER=132547698, CN=Blå &amp; Bjørnebær AS, O=Blå &amp; Bjørnebær AS, C=NO"
//        mottaksId = sendInRequest.messageId
        mottattDatotid = Instant.now().toXmlGregorianCalendar()
        ediLoggId = sendInRequest.messageId
//        avsenderFnrFraDigSignatur = sendInRequest.signedOf ?: "NA"
//        avsenderOrgNrFraDigSignatur = "TODO4"
        herIdentifikator = sendInRequest.addressing.from.partyId.getIdentifikatorByType("HER")
//        orgNummer = sendInRequest.addressing.from.partyId.getIdentifikatorByType("orgnummer", "ENH")
        meldingsType = "xml"
        this.partnerReferanse = partnerReferanse
    }
}

private fun List<PartyId>.getIdentifikatorByType(vararg types: String) =
    this.firstOrNull { types.contains(it.type) }?.value ?: "Ukjent"
