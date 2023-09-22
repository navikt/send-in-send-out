/*
 * This Kotlin source file was generated by the Gradle 'init' task.
 */
package no.nav.emottak.ebms

import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.emottak.ebms.db.Database
import no.nav.emottak.ebms.db.DatabaseConfig
import no.nav.emottak.ebms.db.mapHikariConfig
import no.nav.emottak.ebms.model.EbMSAttachment
import no.nav.emottak.ebms.model.EbMSDocument
import no.nav.emottak.ebms.model.getAttachmentId
import no.nav.emottak.ebms.model.getConversationId
import no.nav.emottak.ebms.processing.EbmsMessageProcessor
import no.nav.emottak.ebms.xml.xmlMarshaller
import org.xmlsoap.schemas.soap.envelope.Envelope


val processor = EbmsMessageProcessor()
fun main() {


    val database = Database(mapHikariConfig(DatabaseConfig()))
    database.migrate()

    embeddedServer(Netty, port = 8080, module = Application::myApplicationModule).start(wait = true)
}

fun PartData.FormItem.payload() : ByteArray {
    return java.util.Base64.getMimeDecoder().decode(this.value)
}

fun Application.myApplicationModule() {
    routing {
        get("/") {
            call.respondText("Hello, world!")
        }
        post("/ebms") {
            val allParts = call.receiveMultipart().readAllParts()
            val dokument = allParts.find {
                it.contentType?.toString() == "text/xml" && it.contentDisposition == null
            }
            val attachments = allParts.filter { it.contentDisposition == ContentDisposition.Attachment }
            val dokumentWithAttachment = EbMSDocument(
                "",
                (dokument as PartData.FormItem).payload(),
                attachments.map {
                    EbMSAttachment(
                        (it as PartData.FormItem).payload(),
                        it.contentType!!.contentType,
                        "contentId"
                    )
                })
            processor.process(dokumentWithAttachment)
            println(dokumentWithAttachment)

            call.respondText("Hello")
        }


        post("/ebmsTest") {
            val allParts = call.receiveMultipart().readAllParts()
            val dokument = allParts.find {
                it.contentType?.toString() == "text/xml" && it.contentDisposition == null
            }
            val envelope =
                xmlMarshaller.unmarshal(String((dokument as PartData.FormItem).payload()), Envelope::class.java)

            val conversationId = envelope.getConversationId()
            println(conversationId)
            val attachmentId = envelope.getAttachmentId()
            println(attachmentId)
            val attachments = allParts
                .filter { it.contentDisposition == ContentDisposition.Attachment }
                .filter { it.headers.get("Content-Id")?.contains(attachmentId, true) ?: false }
                .map { (it as PartData.FormItem).payload() }
                .first()
            println(
                String(attachments)
            )
            call.respondText("Hello2")
        }
    }
}
