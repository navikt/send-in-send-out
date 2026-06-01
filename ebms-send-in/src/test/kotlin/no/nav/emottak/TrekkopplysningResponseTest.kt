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
        println(sendInResponse.payload.toString(Charsets.UTF_8))
        assertTrue(producedPayload.contains(expectedPayloadContent), "Payload")
//        loggDiff(expectedPayloadContent, producedPayload.substring(504))
    }

    val payloadFromResponseXmlFile = """
        <MsgType V="APPREC"/>
        <MIGversion>1.0 2004-11-21</MIGversion>
        <GenDate>2026-03-10T12:56:57</GenDate>
        <Id>f2f0f2f6-f0f3-f1f0-f1f2-f5f6f5f7f2f9</Id>
        <Sender>
            <Role V="SENDER"/>
            <HCP>
                <Inst>
                    <Name>NAV IKT</Name>
                    <Id>79768</Id>
                    <TypeId V="HER" DN="HER-id"/>
                </Inst>
            </HCP>
        </Sender>
        <Receiver>
            <Role V="RECEIVER"/>
            <HCP>
                <Inst>
                    <Name>AIDN AS</Name>
                    <Id>8139944</Id>
                    <TypeId V="HER" DN="HER-id"/>
                    <Dept>
                        <Name>Økonomi og oppgjør</Name>
                        <Id>8142626</Id>
                        <TypeId V="HER" DN="HER-id"/>
                    </Dept>
                </Inst>
            </HCP>
        </Receiver>
        <Status V="2" DN="Avvist"/>
        <Error V="B720007F" S="2.16.578.1.12.4.1.1.8124" DN="Trekkvedtak finnes fra før"/>
        <OriginalMsgId>
            <MsgType V="INNRAPPORTERING_TREKK" DN="Innrapportering av trekk, XML, v1.1"/>
            <IssueDate>2026-03-06T15:22:35Z</IssueDate>
            <Id>7f41c4e9-b6bd-44a3-822b-622332b4e421</Id>
        </OriginalMsgId>
    """
}
