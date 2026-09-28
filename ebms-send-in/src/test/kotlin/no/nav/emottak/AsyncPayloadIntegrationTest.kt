package no.nav.emottak

import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.emottak.legemelding.LegeMeldingService
import no.nav.emottak.util.EventRegistrationService
import no.nav.emottak.utils.kafka.model.EventType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AsyncPayloadIntegrationTest : PayloadIntegrationTestFelles() {

    @Test
    fun `Test Legemelding async forwarding succeeds and registers MESSAGE_SENT_TO_FAGSYSTEM`() {
        val legeMeldingService: LegeMeldingService = mockk()
        every { legeMeldingService.queue } returns "PALE_QUEUE"
        every { legeMeldingService.legemelding(any(), any()) } returns Unit
        val mockEventService: EventRegistrationService = mockk(relaxed = true)

        return ebmsSendInTestApp(
            eventRegistrationService = mockEventService,
            legeMeldingService = legeMeldingService
        ) {
            val capturedConversationId = setupEventMockingService(mockEventService)
            val httpClient = createClient {
                install(ContentNegotiation) { json() }
            }
            val sendInRequest = mockSendInRequest("Legemelding", "Legemelding", "<dummy/>".toByteArray())
            val httpResponse = httpClient.post("/fagmelding/asynkron") {
                header("Authorization", "Bearer ${getToken().serialize()}")
                setBody(sendInRequest)
                contentType(ContentType.Application.Json)
            }

            assertEquals(HttpStatusCode.Accepted, httpResponse.status)
            verify(exactly = 1) { legeMeldingService.legemelding(any(), any()) }
            verify(exactly = 1) {
                mockEventService.registerEvent(
                    EventType.MESSAGE_SENT_TO_FAGSYSTEM,
                    any(),
                    any(),
                    any(),
                    any()
                )
            }
            verify(exactly = 0) {
                mockEventService.registerEvent(
                    EventType.ERROR_WHILE_SENDING_MESSAGE_TO_FAGSYSTEM,
                    any(),
                    any(),
                    any(),
                    any()
                )
            }
            assertEquals(sendInRequest.conversationId, capturedConversationId.captured)
        }
    }

    @Test
    fun `Test Legemelding async forwarding failure registers ERROR_WHILE_SENDING_MESSAGE_TO_FAGSYSTEM`() {
        val legeMeldingService: LegeMeldingService = mockk()
        every { legeMeldingService.legemelding(any(), any()) } throws RuntimeException("MQ unavailable")
        val mockEventService: EventRegistrationService = mockk(relaxed = true)

        return ebmsSendInTestApp(
            eventRegistrationService = mockEventService,
            legeMeldingService = legeMeldingService
        ) {
            setupEventMockingService(mockEventService)
            val httpClient = createClient {
                install(ContentNegotiation) { json() }
            }
            val sendInRequest = mockSendInRequest("Legemelding", "Legemelding", "<dummy/>".toByteArray())
            val httpResponse = httpClient.post("/fagmelding/asynkron") {
                header("Authorization", "Bearer ${getToken().serialize()}")
                setBody(sendInRequest)
                contentType(ContentType.Application.Json)
            }

            assertEquals(HttpStatusCode.BadRequest, httpResponse.status)
            assert(httpResponse.bodyAsText().contains("MQ unavailable"))
            verify(exactly = 1) {
                mockEventService.registerEvent(
                    EventType.ERROR_WHILE_SENDING_MESSAGE_TO_FAGSYSTEM,
                    any(),
                    any(),
                    any(),
                    any()
                )
            }
            verify(exactly = 0) {
                mockEventService.registerEvent(
                    EventType.MESSAGE_SENT_TO_FAGSYSTEM,
                    any(),
                    any(),
                    any(),
                    any()
                )
            }
        }
    }

    @Test
    fun `Test unsupported service on async route registers ERROR_WHILE_SENDING_MESSAGE_TO_FAGSYSTEM`() {
        val mockEventService: EventRegistrationService = mockk(relaxed = true)

        return ebmsSendInTestApp(eventRegistrationService = mockEventService) {
            setupEventMockingService(mockEventService)
            val httpClient = createClient {
                install(ContentNegotiation) { json() }
            }
            val sendInRequest = mockSendInRequest("HarBorgerFrikort", "HarBorgerFrikort", "<dummy/>".toByteArray())
            val httpResponse = httpClient.post("/fagmelding/asynkron") {
                header("Authorization", "Bearer ${getToken().serialize()}")
                setBody(sendInRequest)
                contentType(ContentType.Application.Json)
            }

            assertEquals(HttpStatusCode.BadRequest, httpResponse.status)
            assert(httpResponse.bodyAsText().contains("is not implemented"))
            verify(exactly = 1) {
                mockEventService.registerEvent(
                    EventType.ERROR_WHILE_SENDING_MESSAGE_TO_FAGSYSTEM,
                    any(),
                    any(),
                    any(),
                    any()
                )
            }
            verify(exactly = 0) {
                mockEventService.registerEvent(
                    EventType.MESSAGE_SENT_TO_FAGSYSTEM,
                    any(),
                    any(),
                    any(),
                    any()
                )
            }
        }
    }

    @Test
    fun `Test async route responds with BadRequest on malformed request body`() = ebmsSendInTestApp {
        val httpClient = createClient {
            install(ContentNegotiation) { json() }
        }
        val httpResponse = httpClient.post("/fagmelding/asynkron") {
            header("Authorization", "Bearer ${getToken().serialize()}")
            setBody("""{"invalid":"body"}""")
            contentType(ContentType.Application.Json)
        }

        assertEquals(HttpStatusCode.BadRequest, httpResponse.status)
    }
}
