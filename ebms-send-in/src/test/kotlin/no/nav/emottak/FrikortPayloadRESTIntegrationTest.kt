package no.nav.emottak

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
import no.kith.xmlstds.msghead._2006_05_24.MsgHead
import no.kith.xmlstds.nav.egenandel._2016_06_10.EgenandelSvarV2
import no.nav.emottak.frikort.egenandelForesporselXmlMarshaller
import no.nav.emottak.utils.common.model.SendInResponse
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FrikortPayloadRESTIntegrationTest : PayloadIntegrationTestFelles("FRIKORT_URL_REST") {

    @Test
    fun `Test Frikort-HarBorgerFrikort`() = ebmsSendInTestApp(
        "frikort/rest/EgenandelForesporselV2_harBorgerEgenandelfritak_response.json",
        ContentType.Application.Json
    ) {
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
            setBody(validSendInHarBorgerFrikortRequest.value.copy(cpaId = "nav:cpaId1"))
            contentType(ContentType.Application.Json)
        }
        log.info(httpResponse.bodyAsText())

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
}
