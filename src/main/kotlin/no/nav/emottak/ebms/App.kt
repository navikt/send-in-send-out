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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import no.nav.emottak.constants.SMTPHeaders
import no.nav.emottak.ebms.model.*
import no.nav.emottak.ebms.processing.ProcessingService
import no.nav.emottak.ebms.validation.DokumentValidator
import no.nav.emottak.ebms.validation.MimeHeaders
import no.nav.emottak.ebms.validation.MimeValidationException
import no.nav.emottak.ebms.validation.validateMimeAttachment
import no.nav.emottak.ebms.validation.validateMimeSoapEnvelope
import no.nav.emottak.ebms.xml.asString
import no.nav.emottak.ebms.xml.getDocumentBuilder
import org.slf4j.LoggerFactory
import java.io.ByteArrayInputStream

val log = LoggerFactory.getLogger("no.nav.emottak.ebms.App")

fun logger() = log
fun main() {
    //val database = Database(mapHikariConfig(DatabaseConfig()))
    //database.migrate()
    embeddedServer(Netty, port = 8080, module = Application::ebmsProviderModule).start(wait = true)
}

fun defaultHttpClient(): () -> HttpClient {
    return { HttpClient(CIO) {
        expectSuccess = true
        install(io.ktor.client.plugins.contentnegotiation.ContentNegotiation) {
            json()
        }
    }}
}
fun PartData.payload(clearText:Boolean = false): ByteArray {
    return when (this) {
        is PartData.FormItem ->  if (clearText) return this.value.toByteArray()
        else java.util.Base64.getMimeDecoder().decode(this.value)
        is PartData.FileItem -> {
            val bytes = this.streamProvider.invoke().readAllBytes()
            if (clearText) return bytes else java.util.Base64.getMimeDecoder().decode(bytes)
        }

        else -> byteArrayOf()
    }
}


fun Application.ebmsProviderModule() {
    val client = defaultHttpClient()
    val cpaClient = CpaRepoClient(client)
    val processingClient = PayloadProcessingClient(client)
    val validator = DokumentValidator(cpaClient)
    val processing = ProcessingService(processingClient)

    routing {
        get("/") {
            call.respondText("Hello, world!")
        }
        postEbms(validator, processing)
    }

}

@Throws(MimeValidationException::class)
suspend fun ApplicationCall.receiveEbmsDokument(): EbMSDocument {
    log.info("Parsing message with Message-Id: ${request.header(SMTPHeaders.MESSAGE_ID)}")
    val debugClearText = !request.header("cleartext").isNullOrBlank()
    return when (val contentType = this.request.contentType().withoutParameters()) {
        ContentType.parse("multipart/related") -> {
            val allParts = this.receiveMultipart().readAllParts()
            val dokument = allParts.find {
                it.contentType?.withoutParameters() == ContentType.parse("text/xml") && it.contentDisposition == null
            }.also {
                it?.validateMimeSoapEnvelope()
                    ?: throw MimeValidationException("Unable to find soap envelope multipart Message-Id ${this.request.header(SMTPHeaders.MESSAGE_ID)}")
            }!!.let {
                 val contentID = it.headers[MimeHeaders.CONTENT_ID]!!.convertToValidatedContentID()
                 val isBase64 = "base64" == it.headers[MimeHeaders.CONTENT_TRANSFER_ENCODING]
                 Pair(contentID, it.payload(debugClearText || !isBase64))
            }
            val attachments =
                allParts.filter { it.contentDisposition?.disposition == ContentDisposition.Attachment.disposition }
            attachments.forEach {
                it.validateMimeAttachment()
            }
            EbMSDocument(
                dokument.first,
                getDocumentBuilder().parse(ByteArrayInputStream(dokument.second)),
                attachments.map {
                    EbMSAttachment(
                        it.payload(),
                        it.contentType!!.contentType,
                        it.headers[MimeHeaders.CONTENT_ID]!!.convertToValidatedContentID()
                    )
                })

        }

        ContentType.parse("text/xml") -> {
            val dokument = withContext(Dispatchers.IO) {
                if(debugClearText || "base64" != request.header(MimeHeaders.CONTENT_TRANSFER_ENCODING))
                    this@receiveEbmsDokument.receiveStream().readAllBytes()
                 else
                    java.util.Base64.getMimeDecoder()
                        .decode(this@receiveEbmsDokument.receiveStream().readAllBytes())
            }
            EbMSDocument(
                this.request.headers[MimeHeaders.CONTENT_ID]!!.convertToValidatedContentID(),
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

private fun String.convertToValidatedContentID(): String {
    return Regex("""<(.*?)>""").find(this)?.groups?.get(1)?.value ?: throw MimeValidationException("ContentId is invalid $this")
}

suspend fun ApplicationCall.respondEbmsDokument(ebmsDokument: EbMSDocument) {

    val payload = ebmsDokument
        .dokument
        .asString()
    //  .encodeBase64()
    if (ebmsDokument.dokumentType() == DokumentType.ACKNOWLEDGMENT) {
        log.info("Successfuly processed Payload Message")
    }
    this.respondText(payload, ContentType.parse("text/xml"))
}
