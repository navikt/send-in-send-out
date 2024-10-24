package no.nav.emottak.ebms

import io.ktor.client.call.body
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.server.application.call
import io.ktor.server.request.receiveStream
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import jakarta.xml.soap.SOAPFault
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import no.nav.emottak.message.model.EbmsFail
import no.nav.emottak.message.model.EbmsMessage
import no.nav.emottak.message.model.EbmsProcessing
import no.nav.emottak.message.model.PayloadMessage
import no.nav.emottak.util.getEnvVar

private val ebxmlProcessorEndpoint = getEnvVar("EBXML_PROCESSOR_URL", "http://ebms-provider.team-emottak.svc.nais.local")
fun Route.postEbmsSync(): Route = post("/ebms/sync") {
    val httpClient = defaultHttpClient().invoke()
    val response = httpClient.post("$ebxmlProcessorEndpoint/ebxml/unpack") {
        withContext(Dispatchers.IO) {
            this@post.setBody(call.receiveStream().readAllBytes())
        }
        this.headers {
            call.request.headers.entries().forEach { header ->
                this.appendAll(header.key, header.value)
            }
        }
    }
    val ebxmlProcessingResponse: PayloadMessage = response.body()
    val payloadMessage = Json.encodeToString(PayloadMessage.serializer(),ebxmlProcessingResponse)
    println("payload message is $payloadMessage")
}

// @Serializable
data class EbxmlProcessingResponse(
    val processingResponse: EbmsMessage,
    val ebmsProcessing: EbmsProcessing = EbmsProcessing(),
    val ebxmlFail: EbmsFail,
    val soapFault: SOAPFault
)
