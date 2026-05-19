package no.nav.emottak.service

import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import no.kith.xmlstds.apprec._2004_11_21.AppRec
import no.nav.emottak.ebms.kafka.EbmsOutPayloadProducer
import no.nav.emottak.ebms.kafka.processMessage
import no.nav.emottak.fellesformat.unmarshal
import no.nav.emottak.util.EventRegistrationService
import no.nav.emottak.utils.common.model.SendInResponse
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import kotlin.uuid.Uuid

class ResponseProcessingTest {

    @Test
    fun `Verify conversion of Fellesformat XML to SendInResponse JSON`() = runBlocking {
        val fellesformatXml = this::class.java.classLoader.getResourceAsStream("svar.xml")!!

        val jsonKafkaProducer: EbmsOutPayloadProducer = mockk(relaxed = true)
        val eventRegistrationService: EventRegistrationService = mockk(relaxed = true)
        processMessage("dummyKey", fellesformatXml.readAllBytes(), jsonKafkaProducer, eventRegistrationService)

        val keySentToKafka = slot<String>()
        val payloadSentToKafka = slot<ByteArray>()
        coVerify { jsonKafkaProducer.send(capture(keySentToKafka), capture(payloadSentToKafka)) }

        verifyOkUuid(keySentToKafka.captured, "Unexpected key/ID")

        val jsonString = String(payloadSentToKafka.captured)
        val sendInResponse = Json.decodeFromString<SendInResponse>(jsonString)

        verifyOkUuid(sendInResponse.messageId, "Unexpected messageId")
        verifyOkUuid(sendInResponse.requestId, "Unexpected requestId")

        // Sjekk mot verdier fra svar.xml
        val ediLoggIdInInputXml = "69abb69f-b491-4d34-aeb1-10c02c7b98b6"
        assertEquals(ediLoggIdInInputXml, sendInResponse.refToMessageId)
        val ebXMLSamtaleIdInInputXml = "91e01f3c-b754-4ea3-98fe-07c249661bba"
        assertEquals(ebXMLSamtaleIdInInputXml, sendInResponse.conversationId)
        val partnerReferanseInInputXml = "nav:qass:36181"
        assertEquals(partnerReferanseInInputXml, sendInResponse.cpaId)

        val ebServiceInInputXml = "Trekkopplysning"
        assertEquals(ebServiceInInputXml, sendInResponse.addressing.service)
        val ebRoleInInputXml = "Avvisning" // role og action er byttet om fra fagsystem !!!
        assertEquals(ebRoleInInputXml, sendInResponse.addressing.action)

        val herIdentifikatorInInputXml = "8142626"
        assertEquals(herIdentifikatorInInputXml, sendInResponse.addressing.to.partyId[0].value)
        assertEquals("HER", sendInResponse.addressing.to.partyId[0].type)
        // to.role settes ikke !

        val navHerIdentifikator = "00000" // settes som env var
        assertEquals(navHerIdentifikator, sendInResponse.addressing.from.partyId[0].value)
        assertEquals("HER", sendInResponse.addressing.from.partyId[0].type)
        val ebActionInInputXml = "Ytelsesutbetaler" // role og action er byttet om fra fagsystem !!!
        assertEquals(ebActionInInputXml, sendInResponse.addressing.from.role)

        val innerPayloadString = String(sendInResponse.payload)
        val innerAppRec = unmarshal(innerPayloadString, AppRec::class.java)
        // Det holder å sjekke noen stikkprøver, når teksten lar seg parse som XML til en AppRec
        val innerAppRecIdInInputXml = "f2f0f2f6-f0f3-f1f0-f1f2-f5f6f5f7f2f9"
        assertEquals(innerAppRecIdInInputXml, innerAppRec.id)
        assertEquals("Avvist", innerAppRec.status.dn)
    }

    private fun verifyOkUuid(s: String, message: String) {
        try {
            Uuid.parse(s)
        } catch (e: IllegalArgumentException) {
            Assertions.fail("$message (Invalid UUID): $s")
        }
    }
}
