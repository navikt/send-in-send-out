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
class LegemeldingResponseTest {

    // Verify that a given Legemelding response Fellesformat XML string is converted to the expected SendInResponse

    @Test
    fun verifyResponse() {
        // use example file legemelding_respons.xml
        val responseXml = this::class.java.classLoader.getResourceAsStream("legemelding_respons.xml")!!.readAllBytes().decodeToString()
        val fellesformat = unmarshal(responseXml, EIFellesformat::class.java)
        val sendInResponse = getResponse(fellesformat)

        assertNotNull(sendInResponse.requestId, "requestId")
        assertNotNull(sendInResponse.messageId, "messageId")
        assertEquals("2a4a4a1e-f2e7-41af-8db0-83de06d0fda3", sendInResponse.refToMessageId, "refToMessageId")
        // todo trenger disse. Mulig også orig meldingsid må hentes ut fra payload ??
        assertEquals("", sendInResponse.conversationId, "conversationId")
        assertEquals("", sendInResponse.cpaId, "cpaId")
        assertEquals("Legemelding", sendInResponse.addressing.service, "service")
        assertEquals("Svarmelding", sendInResponse.addressing.action, "action")
        assertEquals("Lege", sendInResponse.addressing.to.role, "to.role")
        assertEquals("Nav", sendInResponse.addressing.from.role, "from.role")
        assertEquals("00000", sendInResponse.addressing.from.partyId.get(0).value, "from.partyId")
        // todo må evt hente to-party ut fra payload
//        assertEquals("", sendInResponse.addressing.to.partyId.get(0).value, "to.partyId")
//        assertEquals("", sendInResponse.addressing.to.partyId.get(0).type, "to.partyId.type")
        val producedPayload = removeWhitespaceBetweenXmlElementsAndMinimiseOtherWhitespace(String(sendInResponse.payload))
        // We get some namespace stuff in the produced response payload, inserted in the example XML
        val expectedPayloadContent = removeWhitespaceBetweenXmlElementsAndMinimiseOtherWhitespace(payloadFromResponseXmlFile)
        assertTrue(producedPayload.contains(expectedPayloadContent), "Payload")
//        loggDiff(expectedPayloadContent, producedPayload.substring(504))
    }

    val payloadFromResponseXmlFile = """
        <ns9:MsgType V="APPREC"/>
        <ns9:MIGversion>1.0 2004-11-21</ns9:MIGversion>
        <ns9:GenDate>2026-05-07T14:27:22.894655420</ns9:GenDate>
        <ns9:Id>2a4a4a1e-f2e7-41af-8db0-83de06d0fda3</ns9:Id>
        <ns9:Sender>
            <ns9:HCP>
                <ns9:Inst>
                    <ns9:Name>NAV</ns9:Name>
                    <ns9:Id>12668</ns9:Id>
                    <ns9:TypeId V="HER" DN="Identifikator fra Helsetjenesteenhetsregisteret (HER-id)"/>
                    <ns9:AdditionalId>
                        <ns9:Id>123456789</ns9:Id>
                        <ns9:Type V="ENH" DN="Organisasjonsnummeret i Enhetsregister (Brønnøysund)"/>
                    </ns9:AdditionalId>
                </ns9:Inst>
            </ns9:HCP>
        </ns9:Sender>
        <ns9:Receiver>
            <ns9:HCP>
                <ns9:Inst>
                    <ns9:Name>Kule helsetjenester AS</ns9:Name>
                    <ns9:Id>223456789</ns9:Id>
                    <ns9:TypeId V="ENH" DN="Organisasjonsnummeret i Enhetsregister (Brønnøysund)"/>
                    <ns9:AdditionalId>
                        <ns9:Id>0123</ns9:Id>
                        <ns9:Type V="HER" DN="Identifikator fra Helsetjenesteenhetsregisteret (HER-id)"/>
                    </ns9:AdditionalId>
                    <ns9:HCPerson>
                        <ns9:Name>Kveldsmat Aktuell</ns9:Name>
                        <ns9:Id>***********</ns9:Id>
                        <ns9:TypeId V="FNR" DN="Fødselsnummer"/>
                    </ns9:HCPerson>
                </ns9:Inst>
            </ns9:HCP>
        </ns9:Receiver>
        <ns9:Status V="1" DN="OK"/>
        <ns9:OriginalMsgId>
            <ns9:MsgType V="Legerklæring ved arbeidsuførhet" DN="LEGEERKL"/>
            <ns9:IssueDate>2026-05-07T14:27:20.759479299</ns9:IssueDate>
            <ns9:Id>dc97c48a-70f2-4ff2-af91-f005648dee4c</ns9:Id>
        </ns9:OriginalMsgId>
    """
}
