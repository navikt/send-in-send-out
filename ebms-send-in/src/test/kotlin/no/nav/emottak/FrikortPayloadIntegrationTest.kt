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
import no.kith.xmlstds.msghead._2006_05_24.MsgHead
import no.kith.xmlstds.nav.egenandel._2016_06_10.EgenandelSvarV2
import no.kith.xmlstds.nav.egenandelmengde._2016_06_10.EgenandelMengdeSvarV2
import no.nav.emottak.frikort.egenandelForesporselXmlMarshaller
import no.nav.emottak.frikort.egenandelMengdeForesporselXmlMarshaller
import no.nav.emottak.utils.common.model.SendInResponse
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FrikortPayloadIntegrationTest : PayloadIntegrationTestFelles("FRIKORT_URL") {

    @Test
    fun `Test Frikort-HarBorgerFrikort`() = ebmsSendInTestApp("frikort/EgenandelForesporsel_HarBorgerFrikortResponse.xml") {
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
            setBody(validSendInHarBorgerFrikortRequest.value)
            contentType(ContentType.Application.Json)
        }

        // Validering av request:
        val req = mockWebServer!!.takeRequest()
        val auth = req.getHeader(HttpHeaders.Authorization)
        assertNotNull(auth)

        // Validering av response:
        assertEquals(HttpStatusCode.OK, httpResponse.status)
        val responsePayload = httpResponse.body<SendInResponse>().payload
        assertNotNull(responsePayload)
        val msgHead = egenandelForesporselXmlMarshaller.unmarshal(String(responsePayload), MsgHead::class.java)
        val response = msgHead.document.map { doc -> doc.refDoc.content.any }.first().first()
        val content = response as EgenandelSvarV2

        // Validating response content
        assertEquals("Informasjon om fritak fra egenandel er ikke tilgjengelig.", content.svarmelding)
        assertEquals("0", content.status.v)
    }

    @Test
    fun `Test Frikort-HarBorgerFrikortMengde`() = ebmsSendInTestApp("frikort/EgenandelMengdeForesporsel_HarBorgerFrikortMengdeResponse.xml") {
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
            setBody(validSendInHarBorgerFrikortMengdeRequest.value)
            contentType(ContentType.Application.Json)
        }

        val req = mockWebServer!!.takeRequest()

        // Validering av request:
        val auth = req.getHeader(HttpHeaders.Authorization)
        assertNotNull(auth)

        // Validering av response:
        assertEquals(HttpStatusCode.OK, httpResponse.status)
        val responsePayload = httpResponse.body<SendInResponse>().payload
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
        val httpResponse = httpClient.post("/fagmelding/synkron") {
            header(
                "Authorization",
                "Bearer ${getToken().serialize()}"
            )
            setBody(validSendInHarBorgerFrikortMengdeRequest.value)
            contentType(ContentType.Application.Json)
        }

        val req = mockWebServer!!.takeRequest()

        // Validering av request:
        val auth = req.getHeader(HttpHeaders.Authorization)
        assertNotNull(auth)

        // Validering av response:
        assertEquals(HttpStatusCode.OK, httpResponse.status)
        val responsePayload = httpResponse.body<SendInResponse>().payload
        assertNotNull(responsePayload)
        val msgHead = egenandelMengdeForesporselXmlMarshaller.unmarshal(String(responsePayload), MsgHead::class.java)
        val response = msgHead.document.map { doc -> doc.refDoc.content.any }.first().first()
        val content = response as EgenandelMengdeSvarV2

        // Validating response content
        assertEquals(0, content.harBorgerFrikortSvar.size)
    }

    @Test
    fun `Test Frikort-HarBorgerEgenandelfritak`() = ebmsSendInTestApp("frikort/EgenandelForesporsel_HarBorgerEgenandelFritakResponse.xml") {
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
            setBody(validSendInHarBorgerEgenandelfritakRequest.value)
            contentType(ContentType.Application.Json)
        }

        // Validering av request:
        val req = mockWebServer!!.takeRequest()
        val auth = req.getHeader(HttpHeaders.Authorization)
        assertNotNull(auth)

        // Validering av response:
        assertEquals(HttpStatusCode.OK, httpResponse.status)
        val responsePayload = httpResponse.body<SendInResponse>().payload
        assertNotNull(responsePayload)
        val msgHead = egenandelForesporselXmlMarshaller.unmarshal(String(responsePayload), MsgHead::class.java)
        val response = msgHead.document.map { doc -> doc.refDoc.content.any }.first().first()
        val content = response as EgenandelSvarV2

        // Validating response content
        assertEquals("Personen er fritatt for egenandel.", content.svarmelding)
        assertEquals("1", content.status.v)
    }
}
