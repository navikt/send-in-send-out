package no.nav.emottak

import com.nimbusds.jwt.SignedJWT
import io.ktor.server.application.Application
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import no.nav.emottak.auth.AZURE_AD_AUTH
import no.nav.emottak.auth.AuthConfig
import no.nav.emottak.ebms.ebmsSendInModule
import no.nav.security.mock.oauth2.MockOAuth2Server
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer

abstract class PayloadIntegrationTestFelles (
    private val envVarEndpoint:String? = null
) {
    private val mockOAuth2Server = MockOAuth2Server().also { it.start(port = 3344) }
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

    protected fun <T> ebmsSendInTestApp(xmlPath: String? = null, testBlock: suspend ApplicationTestBuilder.() -> T) = testApplication {
        wsSoapMock!!.enqueue(
                MockResponse().setBody(
                    String(
                        ClassLoader.getSystemResourceAsStream(xmlPath)!!.readAllBytes()
                    )
                )
            )
        application(Application::ebmsSendInModule)
        testBlock()
    }

    protected fun getToken(audience: String = AuthConfig.getScope()): SignedJWT = mockOAuth2Server.issueToken(
        issuerId = AZURE_AD_AUTH,
        audience = audience,
        subject = "testUser"
    )
}