/*
 * This Kotlin source file was generated by the Gradle 'init' task.
 */
package no.nav.emottak.ebms

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.*
import no.nav.emottak.ebms.model.*
import no.nav.emottak.ebms.processing.ProcessingService
import no.nav.emottak.ebms.validation.DokumentValidator
import no.nav.emottak.ebms.validation.MimeValidationException
import no.nav.emottak.ebms.validation.asParseAsSoapFault
import no.nav.emottak.ebms.validation.validateMime
import no.nav.emottak.ebms.validation.validateMimeAttachment
import no.nav.emottak.ebms.validation.validateMimeSoapEnvelope
import no.nav.emottak.ebms.xml.asString
import no.nav.emottak.ebms.xml.getDocumentBuilder
import no.nav.emottak.melding.model.SignatureDetails
import no.nav.emottak.util.marker
import org.slf4j.LoggerFactory
import java.io.ByteArrayInputStream

val log = LoggerFactory.getLogger("no.nav.emottak.ebms.App")

fun logger() = log
fun main() {
    //val database = Database(mapHikariConfig(DatabaseConfig()))
    //database.migrate()
    embeddedServer(Netty, port = 8080, module = Application::ebmsProviderModule).start(wait = true)
}

fun PartData.payload(debug:Boolean = false): ByteArray {
    return when (this) {
        is PartData.FormItem -> java.util.Base64.getMimeDecoder().decode(this.value)
        is PartData.FileItem -> {
            val bytes = this.streamProvider.invoke().readAllBytes()
            java.util.Base64.getMimeDecoder().decode(bytes)
        }

        else -> byteArrayOf()
    }
}


fun Application.ebmsProviderModule() {
    val client = { HttpClient(CIO) {
            expectSuccess = true
            install(io.ktor.client.plugins.contentnegotiation.ContentNegotiation) {
                json()
            }
        }
    }
    val cpaClient = CpaRepoClient(client)
    val processingClient = PayloadProcessingClient(client)
    val validator = DokumentValidator(cpaClient)
    val processing = ProcessingService(processingClient)

    routing {
        get("/") {
            call.application.environment.log.info("TESTEST")
            call.respondText("Hello, world!")
        }
        postEbms(validator, processing, cpaClient)

    }

}

@Throws(MimeValidationException::class)
suspend fun ApplicationCall.receiveEbmsDokument(): EbMSDocument {

    return when (val contentType = this.request.contentType().withoutParameters()) {
        ContentType.parse("multipart/related") -> {
            val allParts = this.receiveMultipart().readAllParts()
            val dokument = allParts.find {
                it.contentType?.withoutParameters() == ContentType.parse("text/xml") && it.contentDisposition == null
            }.also {
                it?.validateMimeSoapEnvelope()
                    ?: throw MimeValidationException("Unable to find soap envelope multipart")
            }!!.let {
                if (!request.headers["cleartext"].isNullOrBlank())
                    it.payload(true)
                else it.payload()
            }
            val attachments =
                allParts.filter { it.contentDisposition?.disposition == ContentDisposition.Attachment.disposition }
            attachments.forEach {
                it.validateMimeAttachment()
            }
            EbMSDocument(
                "",
                getDocumentBuilder().parse(ByteArrayInputStream(dokument)),
                attachments.map {
                    EbMSAttachment(
                        it.payload(),
                        it.contentType!!.contentType,
                        it.headers["Content-Id"]!!
                    )
                })

        }

        ContentType.parse("text/xml") -> {
            val dokument = java.util.Base64.getMimeDecoder().decode(this.receiveStream().readAllBytes())
            EbMSDocument(
                "",
                getDocumentBuilder().parse(ByteArrayInputStream(dokument)),
                emptyList()
            )
        }

        else -> {
            throw MimeValidationException("Ukjent request body med Content-Type $contentType")
            //call.respond(HttpStatusCode.BadRequest, "Ukjent request body med Content-Type $contentType")
            //return@post
        }
    }
}

suspend fun ApplicationCall.respondEbmsDokument(ebmsDokument: EbMSDocument) {

    val payload = ebmsDokument
        .dokument
        .asString()
    //  .encodeBase64()
    this.respondText(payload, ContentType.parse("text/xml"))
}
