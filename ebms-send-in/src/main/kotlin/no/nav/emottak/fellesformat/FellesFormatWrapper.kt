package no.nav.emottak.fellesformat

import no.kith.xmlstds.msghead._2006_05_24.MsgHead
import no.nav.emottak.melding.model.PartyId
import no.nav.emottak.melding.model.SendInRequest
import no.nav.emottak.util.toXMLGregorianCalendar
import no.trygdeetaten.xml.eiff._1.EIFellesformat
import no.trygdeetaten.xml.eiff._1.ObjectFactory
import java.time.Instant

private val fellesFormatFactory = ObjectFactory()

fun SendInRequest.asEIFellesFormat(): EIFellesformat =
    fellesFormatFactory.createEIFellesformat().apply {
        mottakenhetBlokk = createFellesFormatMottakEnhetBlokk(this@asEIFellesFormat)
        msgHead = unmarshal(this@asEIFellesFormat.payload.toString(Charsets.UTF_8), MsgHead::class.java)
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
        mottattDatotid = Instant.now().toXMLGregorianCalendar()
        ediLoggId = sendInRequest.messageId
        avsenderFnrFraDigSignatur = sendInRequest.signedOf ?: "NA"
        avsenderOrgNrFraDigSignatur = "TODO4"
        herIdentifikator = sendInRequest.addressing.from.partyId.getIdentifikatorByType("HER")
        orgNummer = sendInRequest.addressing.from.partyId.getIdentifikatorByType("orgnummer", "ENH")
        meldingsType = "xml"
        this.partnerReferanse = partnerReferanse
    }
}

private fun List<PartyId>.getIdentifikatorByType(vararg types: String) =
    this.firstOrNull { types.contains(it.type) }?.value ?: "Ukjent"
