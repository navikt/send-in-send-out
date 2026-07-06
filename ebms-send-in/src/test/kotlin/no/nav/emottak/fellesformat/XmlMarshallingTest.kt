package no.nav.emottak.fellesformat

import no.kith.xmlstds.apprec._2004_11_21.AppRec
import no.kith.xmlstds.msghead._2006_05_24.MsgHead
import no.kith.xmlstds.msghead._2006_05_24.MsgInfo
import no.nav.emottak.trekkopplysning.marshalTrekkopplysning
import no.trygdeetaten.xml.eiff._1.EIFellesformat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.w3._2000._09.xmldsig_.SignatureType

class XmlMarshallingTest {

    // XML som fagsystem forventer den, med spesifikke namespacer, ingen prefix, og rekkefølge action-role-service
    val expectedToTrekkopplysningFagsystem = """
        <?xml version='1.0' encoding='UTF-8'?>
        <EI_fellesformat xmlns="http://www.trygdeetaten.no/xml/eiff/1/">
        <MsgHead xmlns="http://www.kith.no/xmlstds/msghead/2006-05-24">
        <MsgInfo/>
        <Signature xmlns="http://www.w3.org/2000/09/xmldsig#"/>
        </MsgHead>
        <MottakenhetBlokk avsender="8142626" meldingsType="xml" ebAction="action" ebRole="role" ebService="service"/>
        <AppRec/>
        </EI_fellesformat>
    """.trimIndent().replace("\n", "")

    @Test
    fun `test xml with specific requirements from fagsystem for Trekkopplysning`() {
        val f = EIFellesformat()
        f.msgHead = MsgHead()
        f.msgHead.msgInfo = MsgInfo()
        f.msgHead.signature = SignatureType()
        f.mottakenhetBlokk = EIFellesformat.MottakenhetBlokk()
        f.mottakenhetBlokk.avsender = "8142626"
        f.mottakenhetBlokk.ebService = "service"
        f.mottakenhetBlokk.ebAction = "action"
        f.mottakenhetBlokk.ebRole = "role"
        f.mottakenhetBlokk.meldingsType = "xml"
        f.appRec = AppRec()

        val xml = marshalTrekkopplysning(f)
        println(xml)
        assertEquals(expectedToTrekkopplysningFagsystem, xml, "produced XML")
    }
}
