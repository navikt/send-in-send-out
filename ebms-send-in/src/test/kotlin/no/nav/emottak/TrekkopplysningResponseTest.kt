package no.nav.emottak

import no.nav.emottak.ebms.service.FagmeldingResponseService.getResponse
import no.nav.emottak.fellesformat.unmarshal
import no.trygdeetaten.xml.eiff._1.EIFellesformat
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class TrekkopplysningResponseTest {

    // Verify that a given Trekkopplysning response Fellesformat XML string is converted to the expected SendInResponse

    @Test
    fun verifyResponse() {
        // use example file svar.xml
        val responseXml = this::class.java.classLoader.getResourceAsStream("svar.xml")!!.readAllBytes().decodeToString()
        val fellesformat = unmarshal(responseXml, EIFellesformat::class.java)
        val sendInResponse = getResponse(fellesformat)

        assertNotNull(sendInResponse.requestId, "requestId")
        assertNotNull(sendInResponse.messageId, "messageId")
        assertEquals("69abb69f-b491-4d34-aeb1-10c02c7b98b6", sendInResponse.refToMessageId, "refToMessageId")
        assertEquals("91e01f3c-b754-4ea3-98fe-07c249661bba", sendInResponse.conversationId, "conversationId")
        assertEquals("nav:qass:36181", sendInResponse.cpaId, "cpaId")
        assertEquals("Trekkopplysning", sendInResponse.addressing.service, "service")
        assertEquals("Avvisning", sendInResponse.addressing.action, "action")
        assertEquals("Fordringshaver", sendInResponse.addressing.to.role, "to.role")
        assertEquals("Ytelsesutbetaler", sendInResponse.addressing.from.role, "from.role")
        assertEquals("00000", sendInResponse.addressing.from.partyId.get(0).value, "from.partyId")
        assertEquals("8142626", sendInResponse.addressing.to.partyId.get(0).value, "to.partyId")
        assertEquals("HER", sendInResponse.addressing.to.partyId.get(0).type, "to.partyId.type")
        val producedPayload = removeWhitespaceBetweenXmlElementsAndMinimiseOtherWhitespace(String(sendInResponse.payload))
        // We get some namespace stuff in the produced response payload, inserted in the example XML
        val expectedPayloadContent = removeWhitespaceBetweenXmlElementsAndMinimiseOtherWhitespace(payloadFromResponseXmlFile)
        assertTrue(producedPayload.contains(expectedPayloadContent), "Payload")
//        loggDiff(expectedPayloadContent, producedPayload.substring(504))
    }

    val payloadFromResponseXmlFile = """
        <ns9:MsgType V="APPREC"/>
        <ns9:MIGversion>1.0 2004-11-21</ns9:MIGversion>
        <ns9:GenDate>2026-03-10T12:56:57</ns9:GenDate>
        <ns9:Id>f2f0f2f6-f0f3-f1f0-f1f2-f5f6f5f7f2f9</ns9:Id>
        <ns9:Sender>
            <ns9:Role V="SENDER"/>
            <ns9:HCP>
                <ns9:Inst>
                    <ns9:Name>NAV IKT</ns9:Name>
                    <ns9:Id>79768</ns9:Id>
                    <ns9:TypeId V="HER" DN="HER-id"/>
                </ns9:Inst>
            </ns9:HCP>
        </ns9:Sender>
        <ns9:Receiver>
            <ns9:Role V="RECEIVER"/>
            <ns9:HCP>
                <ns9:Inst>
                    <ns9:Name>AIDN AS</ns9:Name>
                    <ns9:Id>8139944</ns9:Id>
                    <ns9:TypeId V="HER" DN="HER-id"/>
                    <ns9:Dept>
                        <ns9:Name>Økonomi og oppgjør</ns9:Name>
                        <ns9:Id>8142626</ns9:Id>
                        <ns9:TypeId V="HER" DN="HER-id"/>
                    </ns9:Dept>
                </ns9:Inst>
            </ns9:HCP>
        </ns9:Receiver>
        <ns9:Status V="2" DN="Avvist"/>
        <ns9:Error V="B720007F" S="2.16.578.1.12.4.1.1.8124" DN="Trekkvedtak finnes fra før"/>
        <ns9:OriginalMsgId>
            <ns9:MsgType V="INNRAPPORTERING_TREKK" DN="Innrapportering av trekk, XML, v1.1"/>
            <ns9:IssueDate>2026-03-06T15:22:35Z</ns9:IssueDate>
            <ns9:Id>7f41c4e9-b6bd-44a3-822b-622332b4e421</ns9:Id>
        </ns9:OriginalMsgId>
    """
}
