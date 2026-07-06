package no.nav.emottak

import no.nav.emottak.ebms.service.FagmeldingResponseService.getResponse
import no.nav.emottak.fellesformat.unmarshal
import no.trygdeetaten.xml.eiff._1.EIFellesformat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
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
        assertEquals("", sendInResponse.conversationId, "conversationId")
        assertEquals("", sendInResponse.cpaId, "cpaId")
        assertEquals("Sykmelding", sendInResponse.addressing.service, "service")
        assertEquals("Svar", sendInResponse.addressing.action, "action")
        assertEquals("Sykmelder", sendInResponse.addressing.to.role, "to.role")
        assertEquals("Saksbehandler", sendInResponse.addressing.from.role, "from.role")
        assertEquals("00000", sendInResponse.addressing.from.partyId.get(0).value, "from.partyId")
        val producedPayload = removeWhitespaceBetweenXmlElementsAndMinimiseOtherWhitespace(String(sendInResponse.payload))
        val expectedPayloadContent = removeWhitespaceBetweenXmlElementsAndMinimiseOtherWhitespace(payloadFromResponseXmlFile)
        assertTrue(producedPayload.contains(expectedPayloadContent), "Payload")
    }

    val payloadFromResponseXmlFile = """
        <MsgType V="APPREC"/>
        <MIGversion>1.0 2004-11-21
        </MIGversion>
        <GenDate>2026-05-07T16:03:39.530273151Z</GenDate>
        <Id>2605071803vakl34068.1</Id>
        <Sender>
            <HCP>
                <Inst>
                    <Name>NAV</Name>
                    <Id>79768</Id>
                    <TypeId V="HER" DN="HER-id"/>
                </Inst>
            </HCP>
        </Sender>
        <Receiver>
            <HCP>
                <Inst>
                    <Name>WebMed Ekstern Test - VAKLENDE TAUS KATT AMBOLT</Name>
                    <Id>8139670</Id>
                    <TypeId V="HER" DN="HER-id"/>
                    <AdditionalId>
                        <Id>311029568</Id>
                        <Type V="ENH" DN="Organisasjonsnummeret i Enhetsregister"/>
                    </AdditionalId>
                    <HCPerson>
                        <Name>Webmed Admin</Name>
                        <Id>565601003</Id>
                        <TypeId V="HPR" DN="HPR-nummer"/>
                        <AdditionalId>
                            <Id>8143656</Id>
                            <Type V="HER" DN="HER-id"/>
                        </AdditionalId>
                    </HCPerson>
                </Inst>
            </HCP>
        </Receiver>
        <Status V="2" DN="Avvist"/>
        <Error V="X99" S="2.16.578.1.12.4.1.1.8221"
            DN="Sykmeldingen kan ikke rettes, det må skrives en ny.Pasienten har ikke fått beskjed, men venter på ny sykmelding fra deg. Grunnet følgende:Pasienten er ikke registrert i folkeregisteret"/>
        <OriginalMsgId>
            <MsgType V="SYKMELD" DN="Vurdering av arbeidsmulighet / sykmelding"/>
            <IssueDate>2026-05-07T18:03:05+02:00</IssueDate>
            <Id>fc41c7c2-dd53-4e9d-8615-446ffd748c06</Id>
        </OriginalMsgId>
    """
}
