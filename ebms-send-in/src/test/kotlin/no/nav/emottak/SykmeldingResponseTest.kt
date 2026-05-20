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
class SykmeldingResponseTest {

    // Verify that a given Sykmelding response Fellesformat XML string is converted to the expected SendInResponse

    @Test
    fun verifyResponse() {
        // use example file sykmelding_respons.xml
        val responseXml = this::class.java.classLoader.getResourceAsStream("sykmelding_respons.xml")!!.readAllBytes().decodeToString()
        val fellesformat = unmarshal(responseXml, EIFellesformat::class.java)
        val sendInResponse = getResponse(fellesformat)

        assertNotNull(sendInResponse.requestId, "requestId")
        assertNotNull(sendInResponse.messageId, "messageId")
        assertEquals("2605071803vakl34068.1", sendInResponse.refToMessageId, "refToMessageId")
        // todo trenger disse. Mulig også orig meldingsid må hentes ut fra payload ??
        assertEquals("", sendInResponse.conversationId, "conversationId")
        assertEquals("", sendInResponse.cpaId, "cpaId")
        assertEquals("Sykmelding", sendInResponse.addressing.service, "service")
        assertEquals("Svar", sendInResponse.addressing.action, "action")
        assertEquals("Sykmelder", sendInResponse.addressing.to.role, "to.role")
        assertEquals("Saksbehandler", sendInResponse.addressing.from.role, "from.role")
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
        <ns9:MIGversion>1.0 2004-11-21
        </ns9:MIGversion>
        <ns9:GenDate>2026-05-07T16:03:39.530273151Z</ns9:GenDate>
        <ns9:Id>2605071803vakl34068.1</ns9:Id>
        <ns9:Sender>
            <ns9:HCP>
                <ns9:Inst>
                    <ns9:Name>NAV</ns9:Name>
                    <ns9:Id>79768</ns9:Id>
                    <ns9:TypeId V="HER" DN="HER-id"/>
                </ns9:Inst>
            </ns9:HCP>
        </ns9:Sender>
        <ns9:Receiver>
            <ns9:HCP>
                <ns9:Inst>
                    <ns9:Name>WebMed Ekstern Test - VAKLENDE TAUS KATT AMBOLT</ns9:Name>
                    <ns9:Id>8139670</ns9:Id>
                    <ns9:TypeId V="HER" DN="HER-id"/>
                    <ns9:AdditionalId>
                        <ns9:Id>311029568</ns9:Id>
                        <ns9:Type V="ENH" DN="Organisasjonsnummeret i Enhetsregister"/>
                    </ns9:AdditionalId>
                    <ns9:HCPerson>
                        <ns9:Name>Webmed Admin</ns9:Name>
                        <ns9:Id>565601003</ns9:Id>
                        <ns9:TypeId V="HPR" DN="HPR-nummer"/>
                        <ns9:AdditionalId>
                            <ns9:Id>8143656</ns9:Id>
                            <ns9:Type V="HER" DN="HER-id"/>
                        </ns9:AdditionalId>
                    </ns9:HCPerson>
                </ns9:Inst>
            </ns9:HCP>
        </ns9:Receiver>
        <ns9:Status V="2" DN="Avvist"/>
        <ns9:Error V="X99" S="2.16.578.1.12.4.1.1.8221"
            DN="Sykmeldingen kan ikke rettes, det må skrives en ny.Pasienten har ikke fått beskjed, men venter på ny sykmelding fra deg. Grunnet følgende:Pasienten er ikke registrert i folkeregisteret"/>
        <ns9:OriginalMsgId>
            <ns9:MsgType V="SYKMELD" DN="Vurdering av arbeidsmulighet / sykmelding"/>
            <ns9:IssueDate>2026-05-07T18:03:05+02:00</ns9:IssueDate>
            <ns9:Id>fc41c7c2-dd53-4e9d-8615-446ffd748c06</ns9:Id>
        </ns9:OriginalMsgId>
    """
}
