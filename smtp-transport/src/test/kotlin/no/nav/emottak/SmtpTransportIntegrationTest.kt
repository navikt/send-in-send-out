package no.nav.emottak

import com.nimbusds.jwt.SignedJWT
import com.zaxxer.hikari.HikariDataSource
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import io.micrometer.prometheus.PrometheusConfig
import io.micrometer.prometheus.PrometheusMeterRegistry
import no.nav.emottak.repository.PayloadRepository
import no.nav.emottak.util.Payload
import no.nav.security.mock.oauth2.MockOAuth2Server
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.testcontainers.containers.PostgreSQLContainer
import kotlin.test.Test
import kotlin.test.assertEquals

class SmtpTransportIntegrationTest {

    companion object {
        lateinit var mockOAuth2Server: MockOAuth2Server
        private val dbContainer: PostgreSQLContainer<Nothing> = smtpTransportPostgres("testDb/db.sql")
        lateinit var dbRepository: PayloadRepository

        @JvmStatic
        @BeforeAll
        fun setUpAll() {
            println("=== Initializing MockOAuth2Server ===")
            mockOAuth2Server = MockOAuth2Server().also { it.start(port = 3344) }

            println("=== Initializing Database ===")
            dbContainer.start()
            dbRepository = PayloadRepository(HikariDataSource(dbContainer.testConfiguration()).asPayloadDatabase())
        }

        @JvmStatic
        @AfterAll
        fun tearDownAll() {
            println("=== Stopping MockOAuth2Server ===")
            mockOAuth2Server.shutdown()
            println("=== Stopping Database ===")
            dbContainer.stop()
        }
    }

    private fun <T> smtpTransportTestApp(testBlock: suspend ApplicationTestBuilder.() -> T) = testApplication {
        application(smtpTransportModule(PrometheusMeterRegistry(PrometheusConfig.DEFAULT), dbRepository))
        testBlock()
    }

    @Test
    fun `Hent payload - ett treff`() = smtpTransportTestApp {
        val httpClient = createClient {
            install(ContentNegotiation) {
                json()
            }
        }
        val httpResponse: HttpResponse = httpClient.get("/payload/123") {
            header(
                "Authorization",
                "Bearer ${getToken().serialize()}"
            )
        }
        assertEquals(HttpStatusCode.OK, httpResponse.status)
        println("Response: ${httpResponse.bodyAsText()}")

        val payloads: List<Payload> = httpResponse.body()
        assertEquals(1, payloads.size)

        // TODO: Validere flere felter
    }

    // TODO: Lage flere tester

    private fun getToken(audience: String = AuthConfig.getScope()): SignedJWT = mockOAuth2Server.issueToken(
        issuerId = AZURE_AD_AUTH,
        audience = audience,
        subject = "testUser"
    )
}
