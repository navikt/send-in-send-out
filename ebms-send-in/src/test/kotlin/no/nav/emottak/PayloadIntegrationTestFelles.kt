package no.nav.emottak

import arrow.fx.coroutines.resourceScope
import com.nimbusds.jwt.SignedJWT
import com.sksamuel.hoplite.Masked
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import io.micrometer.prometheus.PrometheusConfig
import io.micrometer.prometheus.PrometheusMeterRegistry
import kotlinx.coroutines.Dispatchers
import no.nav.emottak.auth.AZURE_AD_AUTH
import no.nav.emottak.auth.AuthConfig
import no.nav.emottak.ebms.ebmsSendInModule
import no.nav.emottak.kafka.KafkaTestContainer
import no.nav.emottak.utils.config.Kafka
import no.nav.emottak.utils.config.KeystoreLocation
import no.nav.emottak.utils.config.KeystoreType
import no.nav.emottak.utils.config.SecurityProtocol
import no.nav.emottak.utils.config.TruststoreLocation
import no.nav.emottak.utils.config.TruststoreType
import no.nav.emottak.utils.coroutines.coroutineScope
import no.nav.emottak.utils.kafka.client.EventPublisherClient
import no.nav.emottak.utils.kafka.service.EventLoggingService
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
        protected lateinit var kafkaTestConfig: Kafka

        @JvmStatic
        @BeforeAll
        fun setUpAll() {
            println("=== Initializing MockOAuth2Server ===")
            mockOAuth2Server = MockOAuth2Server().also { it.start(port = 3344) }

            println("=== Initializing KafkaTestContainer ===")
            KafkaTestContainer.start()
            KafkaTestContainer.createTopic("test-topic")
            kafkaTestConfig = buildKafkaTestConfig(KafkaTestContainer.kafkaContainer.bootstrapServers)
        }

        @JvmStatic
        @AfterAll
        fun tearDownAll() {
            println("=== Stopping MockOAuth2Server ===")
            mockOAuth2Server.shutdown()
            println("=== Stopping KafkaTestContainer ===")
            KafkaTestContainer.stop()
        }

        private fun buildKafkaTestConfig(bootstrapServers: String) = Kafka(
            bootstrapServers = bootstrapServers,
            securityProtocol = SecurityProtocol("PLAINTEXT"),
            keystoreType = KeystoreType(""),
            keystoreLocation = KeystoreLocation(""),
            keystorePassword = Masked(""),
            truststoreType = TruststoreType(""),
            truststoreLocation = TruststoreLocation(""),
            truststorePassword = Masked(""),
            groupId = "ebms-send-in",
            topic = "test-topic",
            eventLoggingProducerActive = true
        )
    }

    protected fun <T> ebmsSendInTestApp(
        mockResponsePath: String? = null,
        testBlock: suspend ApplicationTestBuilder.() -> T
    ) = testApplication {
        resourceScope {
            wsSoapMock!!.enqueue(
                MockResponse().setBody(
                    String(
                        ClassLoader.getSystemResourceAsStream(mockResponsePath)!!.readAllBytes()
                    )
                )
            )
            val meterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)

            val kafkaPublisherClient = EventPublisherClient(kafkaTestConfig)
            val eventLoggingService = EventLoggingService(kafkaPublisherClient)

            val eventRegistrationScope = coroutineScope(Dispatchers.IO)

            application {
                ebmsSendInModule(meterRegistry, eventLoggingService, eventRegistrationScope)
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
