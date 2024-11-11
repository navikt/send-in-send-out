package no.nav.emottak

import com.nimbusds.jwt.SignedJWT
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import no.kith.xmlstds.msghead._2006_05_24.MsgHead
import no.nav.ekstern.virkemiddelokonomi.tjenester.utbetaling.v1.FinnUtbetalingListeFeil
import no.nav.ekstern.virkemiddelokonomi.tjenester.utbetaling.v1.FinnUtbetalingListeResponse
import no.nav.emottak.auth.AZURE_AD_AUTH
import no.nav.emottak.auth.AuthConfig
import no.nav.emottak.ebms.ebmsSendInModule
import no.nav.emottak.melding.model.SendInResponse
import no.nav.emottak.utbetaling.unmarshal
import no.nav.security.mock.oauth2.MockOAuth2Server
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class InntektsforesporselPayloadIntegrationTest {

    private val mockOAuth2Server = MockOAuth2Server().also { it.start(port = 3344) }

    private var utbetalingMock: MockWebServer = MockWebServer()
        .also { it.start() }
        .also {
            System.setProperty("UTBETALING_TEST_ENDPOINT", "http://localhost:${it.port}")
        }

    private fun <T> ebmsSendInTestApp(xmlPath: String, testBlock: suspend ApplicationTestBuilder.() -> T) = testApplication {
        utbetalingMock.enqueue(
            MockResponse().setBody(String(
                ClassLoader.getSystemResourceAsStream(xmlPath)!!.readAllBytes()
            ))
        )
        application(Application::ebmsSendInModule)
        testBlock()
    }

    @Test
    fun `Test Inntektsforespørsel normal respons`() = ebmsSendInTestApp("inntektsforesporsel/finnUtbetalingListeResponse_endret_fnr.xml") {

        val httpClient = createClient {
            install(ContentNegotiation) {
                json()
            }
        }
        val httpResponse = httpClient.post("/fagmelding/synkron") {
            header(
                "Authorization",
                "Bearer ${getToken().serialize()}"
            )
            setBody(validSendInInntektforesporselRequest.value)
            contentType(ContentType.Application.Json)
        }
        assertEquals(HttpStatusCode.OK, httpResponse.status)

        val responsePayload = httpResponse.body<SendInResponse>().payload
        assertNotNull(responsePayload)

        val msgHead = unmarshal(String(responsePayload), MsgHead::class.java)
        val response = msgHead.document.map { doc -> doc.refDoc.content.any }.first().first()
        assert(response is FinnUtbetalingListeResponse)

        val finnUtbetalingListeResponse = response as FinnUtbetalingListeResponse
        assertNotNull(finnUtbetalingListeResponse)
        assertEquals(finnUtbetalingListeResponse.response.utbetalingListe.size, 5)
    }

    @Test
    fun `Test Inntektsforespørsel BrukerIkkeFunnet`() = ebmsSendInTestApp("inntektsforesporsel/finnUtbetalingListeResponse_BrukerIkkeFunnet.xml") {

        val httpClient = createClient {
            install(ContentNegotiation) {
                json()
            }
        }
        val httpResponse = httpClient.post("/fagmelding/synkron") {
            header(
                "Authorization",
                "Bearer ${getToken().serialize()}"
            )
            setBody(validSendInInntektforesporselRequest.value)
            contentType(ContentType.Application.Json)
        }
        assertEquals(HttpStatusCode.OK, httpResponse.status)

        val responsePayload = httpResponse.body<SendInResponse>().payload
        assertNotNull(responsePayload)

        val msgHead = unmarshal(String(responsePayload), MsgHead::class.java)
        val response = msgHead.document.map { doc -> doc.refDoc.content.any }.first().first()
        assert(response is FinnUtbetalingListeFeil)

        val fault = response as FinnUtbetalingListeFeil
        assertNotNull(fault)
        assertNotNull(fault.finnUtbetalingListebrukerIkkeFunnet)
        assertNull(fault.finnUtbetalingListebaksystemIkkeTilgjengelig)
        assertNull(fault.finnUtbetalingListeingenTilgangTilEnEllerFlereYtelser)
        assertNull(fault.finnUtbetalingListeugyldigDato)
        assertNull(fault.finnUtbetalingListeugyldigKombinasjonBrukerIdOgBrukertype)
        assertEquals(fault.finnUtbetalingListebrukerIkkeFunnet.errorMessage, "Bruker ikke funnet")
    }

    @Test
    fun `Test Inntektsforespørsel BaksystemIkkeTilgjengelig`() = ebmsSendInTestApp("inntektsforesporsel/finnUtbetalingListeResponse_BaksystemIkkeTilgjengelig.xml") {

        val httpClient = createClient {
            install(ContentNegotiation) {
                json()
            }
        }
        val httpResponse = httpClient.post("/fagmelding/synkron") {
            header(
                "Authorization",
                "Bearer ${getToken().serialize()}"
            )
            setBody(validSendInInntektforesporselRequest.value)
            contentType(ContentType.Application.Json)
        }
        assertEquals(HttpStatusCode.OK, httpResponse.status)

        val responsePayload = httpResponse.body<SendInResponse>().payload
        assertNotNull(responsePayload)

        val msgHead = unmarshal(String(responsePayload), MsgHead::class.java)
        val response = msgHead.document.map { doc -> doc.refDoc.content.any }.first().first()
        assert(response is FinnUtbetalingListeFeil)

        val fault = response as FinnUtbetalingListeFeil
        assertNotNull(fault)
        assertNull(fault.finnUtbetalingListebrukerIkkeFunnet)
        assertNotNull(fault.finnUtbetalingListebaksystemIkkeTilgjengelig)
        assertNull(fault.finnUtbetalingListeingenTilgangTilEnEllerFlereYtelser)
        assertNull(fault.finnUtbetalingListeugyldigDato)
        assertNull(fault.finnUtbetalingListeugyldigKombinasjonBrukerIdOgBrukertype)
        assertEquals(fault.finnUtbetalingListebaksystemIkkeTilgjengelig.errorMessage, "Baksystemet er ikke tilgjengelig")
    }

    @Test
    fun `Test Inntektsforespørsel IngenTilgangTilEnEllerFlereYtelser`() = ebmsSendInTestApp("inntektsforesporsel/finnUtbetalingListeResponse_IngenTilgangTilEnEllerFlereYtelser.xml") {

        val httpClient = createClient {
            install(ContentNegotiation) {
                json()
            }
        }
        val httpResponse = httpClient.post("/fagmelding/synkron") {
            header(
                "Authorization",
                "Bearer ${getToken().serialize()}"
            )
            setBody(validSendInInntektforesporselRequest.value)
            contentType(ContentType.Application.Json)
        }
        assertEquals(HttpStatusCode.OK, httpResponse.status)

        val responsePayload = httpResponse.body<SendInResponse>().payload
        assertNotNull(responsePayload)

        val msgHead = unmarshal(String(responsePayload), MsgHead::class.java)
        val response = msgHead.document.map { doc -> doc.refDoc.content.any }.first().first()
        assert(response is FinnUtbetalingListeFeil)

        val fault = response as FinnUtbetalingListeFeil
        assertNotNull(fault)
        assertNull(fault.finnUtbetalingListebrukerIkkeFunnet)
        assertNull(fault.finnUtbetalingListebaksystemIkkeTilgjengelig)
        assertNotNull(fault.finnUtbetalingListeingenTilgangTilEnEllerFlereYtelser)
        assertNull(fault.finnUtbetalingListeugyldigDato)
        assertNull(fault.finnUtbetalingListeugyldigKombinasjonBrukerIdOgBrukertype)
        assertEquals(fault.finnUtbetalingListeingenTilgangTilEnEllerFlereYtelser.errorMessage, "Ingen tilgang til hemmelig ytelse")
    }

    @Test
    fun `Test Inntektsforespørsel UgyldigDato`() = ebmsSendInTestApp("inntektsforesporsel/finnUtbetalingListeResponse_UgyldigDato.xml") {

        val httpClient = createClient {
            install(ContentNegotiation) {
                json()
            }
        }
        val httpResponse = httpClient.post("/fagmelding/synkron") {
            header(
                "Authorization",
                "Bearer ${getToken().serialize()}"
            )
            setBody(validSendInInntektforesporselRequest.value)
            contentType(ContentType.Application.Json)
        }
        assertEquals(HttpStatusCode.OK, httpResponse.status)

        val responsePayload = httpResponse.body<SendInResponse>().payload
        assertNotNull(responsePayload)

        val msgHead = unmarshal(String(responsePayload), MsgHead::class.java)
        val response = msgHead.document.map { doc -> doc.refDoc.content.any }.first().first()
        assert(response is FinnUtbetalingListeFeil)

        val fault = response as FinnUtbetalingListeFeil
        assertNotNull(fault)
        assertNull(fault.finnUtbetalingListebrukerIkkeFunnet)
        assertNull(fault.finnUtbetalingListebaksystemIkkeTilgjengelig)
        assertNull(fault.finnUtbetalingListeingenTilgangTilEnEllerFlereYtelser)
        assertNotNull(fault.finnUtbetalingListeugyldigDato)
        assertNull(fault.finnUtbetalingListeugyldigKombinasjonBrukerIdOgBrukertype)
        assertEquals(fault.finnUtbetalingListeugyldigDato.errorMessage, "Dato er ikke gyldig")
    }

    @Test
    fun `Test Inntektsforespørsel UgyldigKombinasjonBrukerIdOgBrukertype`() = ebmsSendInTestApp("inntektsforesporsel/finnUtbetalingListeResponse_UgyldigKombinasjonBrukerIdOgBrukertype.xml") {

        val httpClient = createClient {
            install(ContentNegotiation) {
                json()
            }
        }
        val httpResponse = httpClient.post("/fagmelding/synkron") {
            header(
                "Authorization",
                "Bearer ${getToken().serialize()}"
            )
            setBody(validSendInInntektforesporselRequest.value)
            contentType(ContentType.Application.Json)
        }
        assertEquals(HttpStatusCode.OK, httpResponse.status)

        val responsePayload = httpResponse.body<SendInResponse>().payload
        assertNotNull(responsePayload)

        val msgHead = unmarshal(String(responsePayload), MsgHead::class.java)
        val response = msgHead.document.map { doc -> doc.refDoc.content.any }.first().first()
        assert(response is FinnUtbetalingListeFeil)

        val fault = response as FinnUtbetalingListeFeil
        assertNotNull(fault)
        assertNull(fault.finnUtbetalingListebrukerIkkeFunnet)
        assertNull(fault.finnUtbetalingListebaksystemIkkeTilgjengelig)
        assertNull(fault.finnUtbetalingListeingenTilgangTilEnEllerFlereYtelser)
        assertNull(fault.finnUtbetalingListeugyldigDato)
        assertNotNull(fault.finnUtbetalingListeugyldigKombinasjonBrukerIdOgBrukertype)
        assertEquals(fault.finnUtbetalingListeugyldigKombinasjonBrukerIdOgBrukertype.errorMessage, "Ugyldig kombinasjon: PERSON og 9-sifret ident.")
    }

    @Test
    fun `Test Inntektsforespørsel teknisk feil`() = ebmsSendInTestApp("inntektsforesporsel/finnUtbetalingListeResponse_teknisk_feil.xml") {

        val httpClient = createClient {
            install(ContentNegotiation) {
                json()
            }
        }
        val httpResponse = httpClient.post("/fagmelding/synkron") {
            header(
                "Authorization",
                "Bearer ${getToken().serialize()}"
            )
            setBody(validSendInInntektforesporselRequest.value)
            contentType(ContentType.Application.Json)
        }
        assertEquals(HttpStatusCode.BadRequest, httpResponse.status) // TODO: Burde vært HTTP 500? (HttpStatusCode.InternalServerError)

        val responsePayload = httpResponse.bodyAsText()
        assertNotNull(responsePayload)
        assert(responsePayload.contains("com.ibm.websphere.sca.ServiceRuntimeException"))
        assert(responsePayload.contains("Dette er en teknisk feil fra baksystem"))
    }


    private fun getToken(audience: String = AuthConfig.getScope()): SignedJWT = mockOAuth2Server.issueToken(
        issuerId = AZURE_AD_AUTH,
        audience = audience,
        subject = "testUser"
    )
}
