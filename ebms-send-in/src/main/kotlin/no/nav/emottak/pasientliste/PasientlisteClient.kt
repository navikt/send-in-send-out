package no.nav.emottak.pasientliste

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.basicAuth
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.runBlocking
import no.nav.emottak.ebms.log
import no.nav.emottak.fellesformat.FellesFormatXmlMarshaller
import no.nav.emottak.utils.environment.getEnvVar
import no.nav.emottak.utils.environment.getSecret
import no.trygdeetaten.xml.eiff._1.EIFellesformat

object PasientlisteClient {
    private val url = getEnvVar(
        "PASIENTLISTE_URL",
        "https://wasapp-q1.adeo.no/nav-emottak-practitioner-web/remoting/httpreqhandler-practitioner"
    )
    private val secretPath = getEnvVar("SERVICEUSER_SECRET_PATH", "/dummy/path")
    private val username = lazy { getSecret("$secretPath/username", "testUsername") }
    private val password = lazy { getSecret("$secretPath/password", "testPassword") }

    fun sendRequest(request: EIFellesformat): EIFellesformat {
        val marshalledFellesformat = FellesFormatXmlMarshaller.marshal(request)
        val httpClient = HttpClient(CIO)

        log.debug("Sending in HentPasientliste request with body: $marshalledFellesformat")

        val result = runBlocking {
            try {
                httpClient.post(url) {
                    setBody(marshalledFellesformat)
                    contentType(ContentType.Application.Xml)
                    basicAuth(username.value, password.value)
                }.bodyAsText()
            } catch (e: Exception) {
                log.error("PasientlisteForesporsel error", e)
                throw e
            } finally {
                httpClient.close()
            }
        }

        log.debug("PasientlisteForesporsel result: {}", result)

        return FellesFormatXmlMarshaller.unmarshal(result, EIFellesformat::class.java)
    }
}
