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
        assertEquals("Legemelding", sendInResponse.addressing.service, "service")
        assertEquals("Svarmelding", sendInResponse.addressing.action, "action")
        assertEquals("Lege", sendInResponse.addressing.to.role, "to.role")
        assertEquals("Nav", sendInResponse.addressing.from.role, "from.role")
        assertEquals("00000", sendInResponse.addressing.from.partyId.get(0).value, "from.partyId")
        val producedPayload = removeWhitespaceBetweenXmlElementsAndMinimiseOtherWhitespace(String(sendInResponse.payload))
        val expectedPayloadContent = removeWhitespaceBetweenXmlElementsAndMinimiseOtherWhitespace(payloadFromResponseXmlFile)
        assertTrue(producedPayload.contains(expectedPayloadContent), "Payload")
    }

    val payloadFromResponseXmlFile = """
        <MsgType V="APPREC"/>
        <MIGversion>1.0 2004-11-21</MIGversion>
        <GenDate>2026-05-07T14:27:22.894655420</GenDate>
        <Id>2a4a4a1e-f2e7-41af-8db0-83de06d0fda3</Id>
        <Sender>
            <HCP>
                <Inst>
                    <Name>NAV</Name>
                    <Id>12668</Id>
                    <TypeId V="HER" DN="Identifikator fra Helsetjenesteenhetsregisteret (HER-id)"/>
                    <AdditionalId>
                        <Id>123456789</Id>
                        <Type V="ENH" DN="Organisasjonsnummeret i Enhetsregister (Brønnøysund)"/>
                    </AdditionalId>
                </Inst>
            </HCP>
        </Sender>
        <Receiver>
            <HCP>
                <Inst>
                    <Name>Kule helsetjenester AS</Name>
                    <Id>223456789</Id>
                    <TypeId V="ENH" DN="Organisasjonsnummeret i Enhetsregister (Brønnøysund)"/>
                    <AdditionalId>
                        <Id>0123</Id>
                        <Type V="HER" DN="Identifikator fra Helsetjenesteenhetsregisteret (HER-id)"/>
                    </AdditionalId>
                    <HCPerson>
                        <Name>Kveldsmat Aktuell</Name>
                        <Id>***********</Id>
                        <TypeId V="FNR" DN="Fødselsnummer"/>
                    </HCPerson>
                </Inst>
            </HCP>
        </Receiver>
        <Status V="1" DN="OK"/>
        <OriginalMsgId>
            <MsgType V="Legerklæring ved arbeidsuførhet" DN="LEGEERKL"/>
            <IssueDate>2026-05-07T14:27:20.759479299</IssueDate>
            <Id>dc97c48a-70f2-4ff2-af91-f005648dee4c</Id>
        </OriginalMsgId>
    """
}
