package no.nav.emottak

import arrow.fx.coroutines.resourceScope
import com.nimbusds.jwt.SignedJWT
import io.ktor.http.ContentType
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import io.micrometer.prometheus.PrometheusConfig
import io.micrometer.prometheus.PrometheusMeterRegistry
import no.nav.emottak.auth.AZURE_AD_AUTH
import no.nav.emottak.auth.AuthConfig
import no.nav.emottak.util.EventRegistrationServiceFake
import no.nav.security.mock.oauth2.MockOAuth2Server
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll

abstract class PayloadIntegrationTestFelles(
    private val envVarEndpoint: String? = null
) {
    protected var wsSoapMock: MockWebServer? = null

    init {
        if (envVarEndpoint != null) {
            wsSoapMock = MockWebServer()
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
        testBlock: suspend ApplicationTestBuilder.() -> T
    ) = testApplication {
        resourceScope {
            wsSoapMock!!.enqueue(
                MockResponse().setBody(
                    String(
                        ClassLoader.getSystemResourceAsStream(mockResponsePath)!!.readAllBytes()
                    )
                ).setHeader("Content-Type", mockResponseContentType)
            )
            val meterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)

            val eventRegistrationService = EventRegistrationServiceFake()

            application {
                ebmsSendInModule(meterRegistry, eventRegistrationService)
            }
            testBlock()
        }
    }

    protected fun getToken(audience: String = AuthConfig.getScope()): SignedJWT = mockOAuth2Server.issueToken(
        issuerId = AZURE_AD_AUTH,
        audience = audience,
        subject = "testUser"
    )
}
