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

val UKJENT_ID = "Ukjent"

fun SendInRequest.asEIFellesFormat(): EIFellesformat =
    fellesFormatFactory.createEIFellesformat().apply {
        mottakenhetBlokk = createFellesFormatMottakEnhetBlokk(this@asEIFellesFormat)
        msgHead = unmarshal(this@asEIFellesFormat.payload.toString(Charsets.UTF_8), MsgHead::class.java)
    }

fun SendInRequest.asEIFellesFormat_Trekkopplysning(): EIFellesformat =
    fellesFormatFactory.createEIFellesformat().apply {
        mottakenhetBlokk = createFellesFormatMottakEnhetBlokk_Trekkopplysning(this@asEIFellesFormat_Trekkopplysning)
        msgHead = unmarshal(this@asEIFellesFormat_Trekkopplysning.payload.toString(Charsets.UTF_8), MsgHead::class.java)
        logContents(msgHead)
    }

fun SendInRequest.asEIFellesFormat_Sykmelding(): EIFellesformat =
    fellesFormatFactory.createEIFellesformat().apply {
        mottakenhetBlokk = createFellesFormatMottakEnhetBlokk_Sykmelding(this@asEIFellesFormat_Sykmelding)
        msgHead = unmarshal(this@asEIFellesFormat_Sykmelding.payload.toString(Charsets.UTF_8), MsgHead::class.java)
        logContents(msgHead)
    }

fun SendInRequest.asEIFellesFormat_Legemelding(): EIFellesformat =
    fellesFormatFactory.createEIFellesformat().apply {
        mottakenhetBlokk = createFellesFormatMottakEnhetBlokk_Legemelding(this@asEIFellesFormat_Legemelding)
        msgHead = unmarshal(this@asEIFellesFormat_Legemelding.payload.toString(Charsets.UTF_8), MsgHead::class.java)
        logContents(msgHead)
    }

fun SendInRequest.asEIFellesFormatWithFrikort(): EIFellesformat =
    fellesFormatFactory.createEIFellesformat().apply {
        mottakenhetBlokk = createFellesFormatMottakEnhetBlokk(this@asEIFellesFormatWithFrikort)
        msgHead = frikortSporringXmlMarshaller.unmarshal(this@asEIFellesFormatWithFrikort.payload.toString(Charsets.UTF_8), MsgHead::class.java)
    }

private fun createFellesFormatMottakEnhetBlokk(sendInRequest: SendInRequest): EIFellesformat.MottakenhetBlokk {
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
        this.partnerReferanse = sendInRequest.cpaId
    }
}

private fun createFellesFormatMottakEnhetBlokk_Trekkopplysning(sendInRequest: SendInRequest): EIFellesformat.MottakenhetBlokk {
    // Gamle eMottak
    // - sender IKKE avsenderFnrFraDigSignatur, avsenderOrgNrFraDigSignatur, mottaksId, orgnummer. Vi tar med orgnummer.
    // - sender BLANK herIdentifikator, vi velger å ta den med
    // - sender ANNEN verdi for ediLoggId (= mottaksId, a la 2603041315aidn58567.1). Antar at dette er OK.
    // - sender ANNEN verdi for partnerReferanse (= partner-ID fra CPA-tabell, a la 21137). Vi bruker CPA-ID
    // - sender avsenderRef, med verdi = partner_subjectdn i cpa-tabell. Vi sender blankt
    // Usikker på logikken for verdi i avsender. Velger første som finnes av disse: "HER", "ENH", "orgnummer"
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
    }
}

// <MottakenhetBlokk avsender="912719103" avsenderFnrFraDigSignatur="06828399789"
// avsenderRef="OID.2.5.4.97=NTRNO-912719103, CN=PRIDOK AS - TEST, O=PRIDOK AS, C=NO"
// ebAction="Registrering" ebRole="Sykmelder" ebService="Sykmelding" ebXMLSamtaleId="a219014c-9739-4263-983a-6dd9fc82f8f1"
// ediLoggId="2604160914prid26694.1" herIdentifikator="" meldingsType="xml" mottattDatotid="2026-04-16T09:14:27"
// partnerReferanse="22517"/>
private fun createFellesFormatMottakEnhetBlokk_Sykmelding(sendInRequest: SendInRequest): EIFellesformat.MottakenhetBlokk {
    return fellesFormatFactory.createEIFellesformatMottakenhetBlokk().apply {
        ebXMLSamtaleId = sendInRequest.conversationId
        ebAction = sendInRequest.addressing.action
        ebService = sendInRequest.addressing.service
        ebRole = sendInRequest.addressing.from.role
        herIdentifikator = ""
        orgNummer = ""
        avsender = sendInRequest.addressing.from.partyId.getIdentifikatorByType("HER", "ENH", "orgnummer")
        avsenderFnrFraDigSignatur = sendInRequest.signedOf ?: "NA" // todo OK?
        mottattDatotid = Instant.now().toXmlGregorianCalendar()
        ediLoggId = sendInRequest.messageId
        meldingsType = "xml"
        this.partnerReferanse = sendInRequest.cpaId
        avsenderRef = "" // todo OK?
    }
}

// todo antar vi legger på dette for å identifisere nye emottak
const val NYE_EMOTTAK_ID_PREFIX = "nye-emottak-"

// <MottakenhetBlokk ediLoggId="ed63e4e0-6bed-43b1-b99d-74ef5cb2bc47" avsender="12345678910" ebXMLSamtaleId="1234"
// avsenderRef="00123456789" avsenderFnrFraDigSignatur="20086600138" mottattDatotid="2026-04-08T00:00:00.000+02:00"
// ebRole="Lege" ebService="Legemelding" ebAction="Legeerklaring"/>
private fun createFellesFormatMottakEnhetBlokk_Legemelding(sendInRequest: SendInRequest): EIFellesformat.MottakenhetBlokk {
    return fellesFormatFactory.createEIFellesformatMottakenhetBlokk().apply {
        ebXMLSamtaleId = sendInRequest.conversationId
        ebAction = sendInRequest.addressing.action
        ebService = sendInRequest.addressing.service
        ebRole = sendInRequest.addressing.from.role
//        herIdentifikator = ""
//        orgNummer = ""
        avsender = sendInRequest.addressing.from.partyId.getIdentifikatorByType("HER", "ENH", "orgnummer")
        avsenderFnrFraDigSignatur = sendInRequest.signedOf ?: "NA" // todo OK?
        mottattDatotid = Instant.now().toXmlGregorianCalendar()
        ediLoggId = NYE_EMOTTAK_ID_PREFIX + sendInRequest.messageId
//        meldingsType = "xml"
//        this.partnerReferanse = sendInRequest.cpaId
        avsenderRef = "" // todo OK?
    }
}

private fun List<PartyId>.getIdentifikatorByType(vararg types: String) =
    this.firstOrNull { types.contains(it.type) }?.value ?: UKJENT_ID

private fun logContents(msgHead: MsgHead) {
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
