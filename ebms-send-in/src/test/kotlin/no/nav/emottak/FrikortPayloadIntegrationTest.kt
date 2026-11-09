package no.nav.emottak

import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.mockk.mockk
import io.mockk.verify
import no.kith.xmlstds.apprec._2004_11_21.AppRec
import no.kith.xmlstds.msghead._2006_05_24.MsgHead
import no.kith.xmlstds.nav.egenandelmengde._2016_06_10.EgenandelMengdeSvarV2
import no.nav.emottak.config.Configurator
import no.nav.emottak.frikort.FRIKORT_RECEIVE_TIMEOUT_MILLIS
import no.nav.emottak.frikort.egenandelMengdeForesporselXmlMarshaller
import no.nav.emottak.frikort.frikortEndpoint
import no.nav.emottak.util.EventRegistrationService
import no.nav.emottak.util.apprecMarshaller
import no.nav.emottak.utils.common.model.SendInResponse
import okhttp3.mockwebserver.MockResponse
import org.apache.cxf.frontend.ClientProxy
import org.apache.cxf.transport.http.HTTPConduit
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FrikortPayloadIntegrationTest : PayloadIntegrationTestFelles("FRIKORT_URL") {

    @BeforeAll
    fun beforeAll() {
        Configurator.resetMemoizedConfig()
    }

    @Test
    fun `HarBorgerFrikortMengde har utvidet timeout`() {
        val conduit = ClientProxy.getClient(frikortEndpoint).conduit as HTTPConduit

        assertEquals(FRIKORT_RECEIVE_TIMEOUT_MILLIS, conduit.client.receiveTimeout)
    }

    @Test
    fun `HarBorgerFrikortMengde returns AppRec on SOAP client fault`() = ebmsSendInTestApp(
        eventRegistrationService = mockk<EventRegistrationService>(relaxed = true)
    ) { eventRegistrationService ->
        mockWebServer!!.enqueue(
            MockResponse()
                .setResponseCode(500)
                .setHeader(HttpHeaders.ContentType, "text/xml")
                .setBody(
                    """
                    <soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
                        <soap:Body>
                            <soap:Fault>
                                <faultcode>soap:Client</faultcode>
                                <faultstring>Frikort service failed</faultstring>
                            </soap:Fault>
                        </soap:Body>
                    </soap:Envelope>
                    """.trimIndent()
                )
        )
        val httpClient = createClient {
            install(ContentNegotiation) {
                json()
            }
        }
        val sendInRequest = validSendInHarBorgerFrikortMengdeRequest.value
        val httpResponse = httpClient.post("/fagmelding/synkron") {
            header(HttpHeaders.Authorization, "Bearer ${getToken().serialize()}")
            setBody(sendInRequest)
            contentType(ContentType.Application.Json)
        }
        mockWebServer!!.takeRequest()

        assertEquals(HttpStatusCode.OK, httpResponse.status)
        val response = httpResponse.body<SendInResponse>()
        assertEquals(sendInRequest.messageId, response.refToMessageId)
        assertEquals(sendInRequest.conversationId, response.conversationId)
        assertEquals(sendInRequest.cpaId, response.cpaId)
        assertNotEquals(sendInRequest.messageId, response.messageId)
        assertEquals(sendInRequest.addressing.replyTo("HarBorgerFrikortMengde", "Avvisning"), response.addressing)
        assertNotNull(apprecMarshaller.unmarshal(String(response.payload), AppRec::class.java))
        verify(exactly = 0) { eventRegistrationService.registerErrorOnHandoverToFagsystem(sendInRequest, any()) }
        verify(exactly = 1) {
            eventRegistrationService.registerEventMessageDetails(
                match {
                    it.copy(payload = response.payload) == response && it.payload.contentEquals(response.payload)
                }
            )
        }
    }

    @Test
    fun `HarBorgerFrikortMengde returns Apprec on SOAP server fault`() = ebmsSendInTestApp(
        eventRegistrationService = mockk<EventRegistrationService>(relaxed = true)
    ) { eventRegistrationService ->
        mockWebServer!!.enqueue(
            MockResponse()
                .setResponseCode(500)
                .setHeader(HttpHeaders.ContentType, "text/xml")
                .setBody(
                    """
                    <soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
                        <soap:Body>
                            <soap:Fault>
                                <faultcode>soap:Server</faultcode>
                                <faultstring>Frikort service failed</faultstring>
                            </soap:Fault>
                        </soap:Body>
                    </soap:Envelope>
                    """.trimIndent()
                )
        )
        val httpClient = createClient {
            install(ContentNegotiation) {
                json()
            }
        }
        val sendInRequest = validSendInHarBorgerFrikortMengdeRequest.value
        val httpResponse = httpClient.post("/fagmelding/synkron") {
            header(HttpHeaders.Authorization, "Bearer ${getToken().serialize()}")
            setBody(sendInRequest)
            contentType(ContentType.Application.Json)
        }
        mockWebServer!!.takeRequest()

        assertEquals(HttpStatusCode.InternalServerError, httpResponse.status)
        verify(exactly = 1) { eventRegistrationService.registerErrorOnHandoverToFagsystem(any(), any()) }
    }

    @Test
    fun `HarBorgerFrikortMengde does not return AppRec for non SOAP failures`() = ebmsSendInTestApp(
        eventRegistrationService = mockk<EventRegistrationService>(relaxed = true)
    ) { eventRegistrationService ->
        mockWebServer!!.enqueue(
            MockResponse()
                .setResponseCode(503)
                .setHeader(HttpHeaders.ContentType, "text/plain")
                .setBody("Service unavailable")
        )
        val httpClient = createClient {
            install(ContentNegotiation) {
                json()
            }
        }
        val sendInRequest = validSendInHarBorgerFrikortMengdeRequest.value
        val httpResponse = httpClient.post("/fagmelding/synkron") {
            header(HttpHeaders.Authorization, "Bearer ${getToken().serialize()}")
            setBody(sendInRequest)
            contentType(ContentType.Application.Json)
        }
        mockWebServer!!.takeRequest()

        assertEquals(HttpStatusCode.BadRequest, httpResponse.status)
        verify(exactly = 1) { eventRegistrationService.registerErrorOnHandoverToFagsystem(any(), any()) }
        verify(exactly = 0) { eventRegistrationService.registerEventMessageDetails(any()) }
    }

    @Test
    fun `Test Frikort-HarBorgerFrikortMengde`() = ebmsSendInTestApp("frikort/EgenandelMengdeForesporsel_HarBorgerFrikortMengdeResponse.xml") {
        val httpClient = createClient {
            install(ContentNegotiation) {
                json()
            }
        }
        val sendInRequest = validSendInHarBorgerFrikortMengdeRequest.value.copy(cpaId = "nav:70079")
        val httpResponse = httpClient.post("/fagmelding/synkron") {
            header(
                "Authorization",
                "Bearer ${getToken().serialize()}"
            )
            setBody(sendInRequest)
            contentType(ContentType.Application.Json)
        }

        val req = mockWebServer!!.takeRequest()

        // Validering av request:
        val auth = req.getHeader(HttpHeaders.Authorization)
        assertNotNull(auth)

        assertEquals("HarBorgerFrikortMengde", sendInRequest.addressing.service, "Request service mismatch")
        assertEquals("EgenandelMengdeForesporsel", sendInRequest.addressing.action, "Request action mismatch")
        assertEquals("Behandler", sendInRequest.addressing.from.role, "Request from role mismatch")

        // Validering av response:
        assertEquals(HttpStatusCode.OK, httpResponse.status)
        val sendInResponse: SendInResponse = httpResponse.body<SendInResponse>()
        assertEquals(sendInRequest.conversationId, sendInResponse.conversationId)
        assertNotEquals(sendInRequest.messageId, sendInResponse.messageId)
        assertEquals("HarBorgerFrikortMengde", sendInResponse.addressing.service, "Response service mismatch")
        assertEquals("Svar", sendInResponse.addressing.action, "Response action mismatch")
        assertEquals("Frikortregister", sendInResponse.addressing.from.role, "Response from role mismatch")

        val responsePayload = sendInResponse.payload
        assertNotNull(responsePayload)
        val msgHead = egenandelMengdeForesporselXmlMarshaller.unmarshal(String(responsePayload), MsgHead::class.java)
        val response = msgHead.document.map { doc -> doc.refDoc.content.any }.first().first()
        val content = response as EgenandelMengdeSvarV2

        // Validating response content
        assertEquals(2, content.harBorgerFrikortSvar.size)
        assertEquals("0", content.harBorgerFrikortSvar[0].status.v)
        assertEquals("1", content.harBorgerFrikortSvar[1].status.v)
    }

    @Test
    fun `Test Frikort-HarBorgerFrikortMengde tom liste`() = ebmsSendInTestApp("frikort/EgenandelMengdeForesporsel_HarBorgerFrikortMengdeResponse_tomListe.xml") {
        val httpClient = createClient {
            install(ContentNegotiation) {
                json()
            }
        }
        val sendInRequest = validSendInHarBorgerFrikortMengdeRequest.value.copy(cpaId = "nav:70079")
        val httpResponse = httpClient.post("/fagmelding/synkron") {
            header(
                "Authorization",
                "Bearer ${getToken().serialize()}"
            )
            setBody(sendInRequest)
            contentType(ContentType.Application.Json)
        }

        val req = mockWebServer!!.takeRequest()

        // Validering av request:
        val auth = req.getHeader(HttpHeaders.Authorization)
        assertNotNull(auth)

        assertEquals("HarBorgerFrikortMengde", sendInRequest.addressing.service, "Request service mismatch")
        assertEquals("EgenandelMengdeForesporsel", sendInRequest.addressing.action, "Request action mismatch")
        assertEquals("Behandler", sendInRequest.addressing.from.role, "Request from role mismatch")

        // Validering av response:
        assertEquals(HttpStatusCode.OK, httpResponse.status)
        val sendInResponse: SendInResponse = httpResponse.body<SendInResponse>()
        assertEquals(sendInRequest.conversationId, sendInResponse.conversationId)
        assertNotEquals(sendInRequest.messageId, sendInResponse.messageId)
        assertEquals("HarBorgerFrikortMengde", sendInResponse.addressing.service, "Response service mismatch")
        assertEquals("Svar", sendInResponse.addressing.action, "Response action mismatch")
        assertEquals("Frikortregister", sendInResponse.addressing.from.role, "Response from role mismatch")

        val responsePayload = httpResponse.body<SendInResponse>().payload
        assertNotNull(responsePayload)
        val msgHead = egenandelMengdeForesporselXmlMarshaller.unmarshal(String(responsePayload), MsgHead::class.java)
        val response = msgHead.document.map { doc -> doc.refDoc.content.any }.first().first()
        val content = response as EgenandelMengdeSvarV2

        // Validating response content
        assertEquals(0, content.harBorgerFrikortSvar.size)
    }

    @Test
    fun `Test Frikort-HarBorgerFrikortMengde sender conversationId til eventmanager`() = ebmsSendInTestApp(
        mockResponsePath = "frikort/EgenandelMengdeForesporsel_HarBorgerFrikortMengdeResponse_tomListe.xml",
        eventRegistrationService = mockk<EventRegistrationService>(relaxed = true)
    ) { mockEventRegistrationService ->
        val capturedConversationId = setupEventMockingService(mockEventRegistrationService)
        val httpClient = createClient {
            install(ContentNegotiation) {
                json()
            }
        }
        val sendInRequest = validSendInHarBorgerFrikortMengdeRequest.value
        val httpResponse = httpClient.post("/fagmelding/synkron") {
            header(
                "Authorization",
                "Bearer ${getToken().serialize()}"
            )
            setBody(sendInRequest)
            contentType(ContentType.Application.Json)
        }
        validateEventMockingResponse(mockEventRegistrationService, httpResponse, capturedConversationId, 3)
    }
}
