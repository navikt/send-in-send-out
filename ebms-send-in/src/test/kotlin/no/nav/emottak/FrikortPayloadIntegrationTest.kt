package no.nav.emottak

import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import no.kith.xmlstds.msghead._2006_05_24.MsgHead
import no.kith.xmlstds.nav.egenandelmengde._2016_06_10.EgenandelMengdeSvarV2
import no.nav.emottak.melding.model.SendInResponse
import no.nav.emottak.utbetaling.unmarshal
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FrikortPayloadIntegrationTest : PayloadIntegrationTestFelles("FRIKORT_URL") {

    @Test
    fun `Test Frikort-HarBorgerFrikort normal respons`() = ebmsSendInTestApp("frikort/EgenandelForesporsel_HarBorgerFrikortResponse.xml") {
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

        val req = wsSoapMock!!.takeRequest()
        println("REQUEST:\n${req.body.readByteString().utf8()}\n\nRESPONSE:\n${httpResponse.bodyAsText()}")

        // Validering av request:
        val auth = req.getHeader(HttpHeaders.Authorization)
        println("Basic Auth:\n$auth")
        assertNotNull(auth)

        // Validering av response:
        assertEquals(HttpStatusCode.OK, httpResponse.status)

        val responsePayload = httpResponse.body<SendInResponse>().payload
        assertNotNull(responsePayload)
        println("responsePayload som string:\n" + String(responsePayload))
        // TODO: Verifisere at Content-tag er i henhold til XSD (NAV-Egenandel-2016-06-10.xsd fra emottak-payload-xsd)
    }

    @Test
    fun `Test Frikort-HarBorgerFrikortMengde normal respons`() = ebmsSendInTestApp("frikort/EgenandelMengdeForesporsel_HarBorgerFrikortMengdeResponse.xml") {
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

        val req = wsSoapMock!!.takeRequest()
        println("REQUEST:\n${req.body.readByteString().utf8()}\n\nRESPONSE:\n${httpResponse.bodyAsText()}")

        // Validering av request:
        val auth = req.getHeader(HttpHeaders.Authorization)
        println("Basic Auth:\n$auth")
        assertNotNull(auth)

        // Validering av response:
        assertEquals(HttpStatusCode.OK, httpResponse.status)

        val responsePayload = httpResponse.body<SendInResponse>().payload
        assertNotNull(responsePayload)
        println("responsePayload som string:\n" + String(responsePayload))
        // TODO: Verifisere at Content-tag er i henhold til XSD (NAV-EgenandelMengde-2016-06-10.xsd fra emottak-payload-xsd)


        val msgHead = unmarshal(String(responsePayload), MsgHead::class.java)
        println(msgHead)
        val response = msgHead.document.map { doc -> doc.refDoc.content.any }.first().first()
        println(response.javaClass.name)
        assertTrue(response is EgenandelMengdeSvarV2)
        //assert(String(responsePayload).contains("EgenandelMengdeSvarV2"))
    }
}
