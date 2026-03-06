package no.nav.emottak

import arrow.fx.coroutines.resourceScope
import com.nimbusds.jwt.SignedJWT
import io.ktor.client.call.body
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import io.micrometer.prometheus.PrometheusConfig
import io.micrometer.prometheus.PrometheusMeterRegistry
import io.mockk.CapturingSlot
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.emottak.auth.AZURE_AD_AUTH
import no.nav.emottak.auth.AuthConfig
import no.nav.emottak.trekkopplysning.TrekkopplysningService
import no.nav.emottak.util.EventRegistrationService
import no.nav.emottak.util.EventRegistrationServiceFake
import no.nav.emottak.utils.common.model.SendInResponse
import no.nav.emottak.utils.kafka.model.EventType
import no.nav.security.mock.oauth2.MockOAuth2Server
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll

abstract class PayloadIntegrationTestFelles(
    private val envVarEndpoint: String? = null
) {
    protected var mockWebServer: MockWebServer? = null

    init {
        if (envVarEndpoint != null) {
            mockWebServer = MockWebServer()
                .also { it.start() }
                .also {
                    System.setProperty(envVarEndpoint, "http://localhost:${it.port}")
                }
        }
    }

    companion object {
        protected lateinit var mockOAuth2Server: MockOAuth2Server

        @JvmStatic
        @BeforeAll
        fun setUpAll() {
            println("=== Initializing MockOAuth2Server ===")
            mockOAuth2Server = MockOAuth2Server().also { it.start(port = 3344) }
        }

        @JvmStatic
        @AfterAll
        fun tearDownAll() {
            println("=== Stopping MockOAuth2Server ===")
            mockOAuth2Server.shutdown()
        }
    }

    protected fun <T> ebmsSendInTestApp(
        mockResponsePath: String? = null,
        mockResponseContentType: ContentType = ContentType.Application.Xml,
        eventRegistrationService: EventRegistrationService = EventRegistrationServiceFake(),
        testBlock: suspend ApplicationTestBuilder.(eventRegistrationService: EventRegistrationService) -> T
    ) = testApplication {
        resourceScope {
            if (mockResponsePath != null) {
                mockWebServer!!.enqueue(
                    MockResponse().setBody(
                        String(
                            ClassLoader.getSystemResourceAsStream(mockResponsePath)!!.readAllBytes()
                        )
                    ).setHeader("Content-Type", mockResponseContentType)
                )
            }
            val meterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)

            val trekkopplysningService: TrekkopplysningService = mockk()
            application {
                ebmsSendInModule(meterRegistry, eventRegistrationService, trekkopplysningService)
            }
            testBlock(eventRegistrationService)
        }
    }

    protected fun getToken(audience: String = AuthConfig.getScope()): SignedJWT = mockOAuth2Server.issueToken(
        issuerId = AZURE_AD_AUTH,
        audience = audience,
        subject = "testUser"
    )

    protected fun setupEventMockingService(mockEventService: EventRegistrationService): CapturingSlot<String> {
        val capturedConversationId = slot<String>()
        every { mockEventService.registerEventMessageDetails(any(), any()) } returns Unit
        every {
            mockEventService.registerEvent(
                eventType = any<EventType>(),
                requestId = any(),
                messageId = any(),
                eventData = any(),
                conversationId = capture(capturedConversationId)
            )
        } returns Unit
        return capturedConversationId
    }

    protected suspend fun validateEventMockingResponse(
        mockEventService: EventRegistrationService,
        httpResponse: HttpResponse,
        capturedConversationId: CapturingSlot<String>,
        expectedNumberOfRegisterEventCalls: Int
    ) {
        mockWebServer!!.takeRequest() // Take out our request to prevent messing up other tests
        assertEquals(HttpStatusCode.OK, httpResponse.status)
        val sendInResponse: SendInResponse = httpResponse.body<SendInResponse>()
        verify(exactly = 1) { mockEventService.registerEventMessageDetails(any(), any()) }
        verify(exactly = expectedNumberOfRegisterEventCalls) { mockEventService.registerEvent(any(), any(), any(), any(), any()) }
        assertTrue(capturedConversationId.captured == sendInResponse.conversationId) {
            "RegisterEvent should have received '${sendInResponse.conversationId}' as conversationId, but got: '${capturedConversationId.captured}'"
        }
    }
}
