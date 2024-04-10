/*
 * This Kotlin source file was generated by the Gradle 'init' task.
 */
package no.nav.emottak.ebms

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.http.ContentType
import io.ktor.http.HeadersBuilder
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.http.content.streamProvider
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.application.createRouteScopedPlugin
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.metrics.micrometer.MicrometerMetrics
import io.ktor.server.netty.Netty
import io.ktor.server.request.contentType
import io.ktor.server.request.header
import io.ktor.server.request.receive
import io.ktor.server.request.receiveMultipart
import io.ktor.server.request.uri
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.util.logging.KtorSimpleLogger
import io.micrometer.prometheus.PrometheusConfig
import io.micrometer.prometheus.PrometheusMeterRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.logstash.logback.marker.Markers
import no.nav.emottak.constants.SMTPHeaders
import no.nav.emottak.ebms.model.DokumentType
import no.nav.emottak.ebms.model.EbMSDocument
import no.nav.emottak.ebms.processing.ProcessingService
import no.nav.emottak.ebms.sendin.SendInService
import no.nav.emottak.ebms.validation.DokumentValidator
import no.nav.emottak.ebms.validation.MimeHeaders
import no.nav.emottak.ebms.validation.MimeValidationException
import no.nav.emottak.ebms.validation.validateMimeAttachment
import no.nav.emottak.ebms.validation.validateMimeSoapEnvelope
import no.nav.emottak.ebms.xml.asString
import no.nav.emottak.ebms.xml.getDocumentBuilder
import no.nav.emottak.melding.model.EbmsAttachment
import org.slf4j.LoggerFactory
import java.io.ByteArrayInputStream
import java.time.Duration
import java.time.Instant
import java.util.*
import kotlin.time.toKotlinDuration

val log = LoggerFactory.getLogger("no.nav.emottak.ebms.App")

fun logger() = log
fun main() {
    // val database = Database(mapHikariConfig(DatabaseConfig()))
    // database.migrate()
    System.setProperty("io.ktor.http.content.multipart.skipTempFile", "true")
    embeddedServer(Netty, port = 8080, module = Application::ebmsProviderModule, configure = {
        this.maxChunkSize = 100000
    }).start(wait = true)
}

fun defaultHttpClient(): () -> HttpClient {
    return {
        HttpClient(CIO) {
            expectSuccess = true
            install(io.ktor.client.plugins.contentnegotiation.ContentNegotiation) {
                json()
            }
        }
    }
}

fun PartData.payload(clearText: Boolean = false): ByteArray {
    return when (this) {
        is PartData.FormItem -> if (clearText) {
            return this.value.toByteArray()
        } else {
            Base64.getMimeDecoder().decode(this.value)
        }

        is PartData.FileItem -> {
            val stream = this.streamProvider.invoke()
            val bytes = stream.readAllBytes()
            stream.close()
            if (clearText) return bytes else Base64.getMimeDecoder().decode(bytes)
        }
        else -> byteArrayOf()
    }
}

fun Application.ebmsProviderModule() {
    val client = defaultHttpClient()
    val cpaClient = CpaRepoClient(client)
    val processingClient = PayloadProcessingClient(client)
    val sendInClient = SendInClient(client)
    val validator = DokumentValidator(cpaClient)
    val processing = ProcessingService(processingClient)
    val sendInService = SendInService(sendInClient)

    val appMicrometerRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
    installMicrometerRegistry(appMicrometerRegistry)
    installRequestTimerPlugin()

    routing {
        get("/") {
            call.respondText("Hello, world!")
        }
        registerHealthEndpoints(appMicrometerRegistry)
        postEbmsAsync(validator, processing)
        postEbmsSync(validator, processing, sendInService)
    }
}

private fun Application.installMicrometerRegistry(appMicrometerRegistry: PrometheusMeterRegistry) {
    install(MicrometerMetrics) {
        registry = appMicrometerRegistry
    }
}

private fun Application.installRequestTimerPlugin() {
    install(
        createRouteScopedPlugin("RequestTimer") {
            val simpleLogger = KtorSimpleLogger("RequestTimerLogger")
            var startTime = Instant.now()
            onCall { call ->
                startTime = Instant.now()
                simpleLogger.info("Received " + call.request.uri)
            }
            onCallRespond { call ->
                val endTime = Duration.between(
                    startTime,
                    Instant.now()
                )
                simpleLogger.info(
                    Markers.appendEntries(
                        mapOf(
                            Pair("Headers", call.request.headers.toString()),
                            Pair("smtpMessageId", call.request.headers[SMTPHeaders.MESSAGE_ID] ?: "-"),
                            Pair("Endpoint", call.request.uri),
                            Pair("RequestTime", endTime.toMillis()),
                            Pair("httpStatus", call.response.status()?.value ?: 0)
                        )
                    ),
                    "Finished " + call.request.uri + " request. Processing time: " + endTime.toKotlinDuration()
                )
            }
        }
    )
}

@Throws(MimeValidationException::class)
suspend fun ApplicationCall.receiveEbmsDokument(): EbMSDocument {
    log.info("Parsing message with Message-Id: ${request.header(SMTPHeaders.MESSAGE_ID)}")
    val debugClearText = !request.header("cleartext").isNullOrBlank()
    return when (val contentType = this.request.contentType().withoutParameters()) {
        ContentType.parse("multipart/related") -> {
            val allParts = mutableListOf<PartData>().apply {
                this@receiveEbmsDokument.receiveMultipart().forEachPart {
                    if (it is PartData.FileItem) it.streamProvider.invoke()
                    if (it is PartData.FormItem) it.value
                    this.add(it)
                    it.dispose.invoke()
                }
            }
            val start = contentType.parameter("start") ?: allParts.first().headers[MimeHeaders.CONTENT_ID]
            val dokument = allParts.find {
                it.headers[MimeHeaders.CONTENT_ID] == start
            }!!.also {
                it.validateMimeSoapEnvelope()
            }.let {
                val contentID = it.headers[MimeHeaders.CONTENT_ID]?.convertToValidatedContentID() ?: "GENERERT-${UUID.randomUUID()}"
                val isBase64 = "base64" == it.headers[MimeHeaders.CONTENT_TRANSFER_ENCODING]
                Pair(contentID, it.payload(debugClearText || !isBase64))
            }
            val attachments =
                allParts.filter { it.headers[MimeHeaders.CONTENT_ID] != start }
            attachments.forEach {
                it.validateMimeAttachment()
            }
            EbMSDocument(
                dokument.first,
                getDocumentBuilder().parse(ByteArrayInputStream(dokument.second)),
                attachments.map {
                    val isBase64 = "base64" == it.headers[MimeHeaders.CONTENT_TRANSFER_ENCODING]
                    EbmsAttachment(
                        it.payload(debugClearText || !isBase64),
                        it.contentType!!.contentType,
                        it.headers[MimeHeaders.CONTENT_ID]!!.convertToValidatedContentID()
                    )
                }
            )
        }

        ContentType.parse("text/xml") -> {
            val dokument = withContext(Dispatchers.IO) {
                if (debugClearText || "base64" != request.header(MimeHeaders.CONTENT_TRANSFER_ENCODING)?.lowercase()) {
                    this@receiveEbmsDokument.receive<ByteArray>()
                } else {
                    Base64.getMimeDecoder()
                        .decode(this@receiveEbmsDokument.receive<ByteArray>())
                }
            }
            EbMSDocument(
                this.request.headers[MimeHeaders.CONTENT_ID]!!.convertToValidatedContentID(),
                getDocumentBuilder().parse(ByteArrayInputStream(dokument)),
                emptyList()
            )
        }

        else -> {
            throw MimeValidationException("Ukjent request body med Content-Type $contentType")
            // call.respond(HttpStatusCode.BadRequest, "Ukjent request body med Content-Type $contentType")
            // return@post
        }
    }
}

private fun String.convertToValidatedContentID(): String {
    return Regex("""<(.*?)>""").find(this)?.groups?.get(1)?.value ?: this
}

suspend fun ApplicationCall.respondEbmsDokument(ebmsDokument: EbMSDocument) {
    if (ebmsDokument.dokumentType() == DokumentType.ACKNOWLEDGMENT) {
        log.info("Successfuly processed Payload Message")
    }
    val ebxml = Base64.getMimeEncoder().encodeToString(ebmsDokument.dokument.asString().toByteArray())

    this.response.headers.apply {
        this.append(MimeHeaders.CONTENT_TYPE, ContentType.Text.Xml.toString())
    }
    if (ebmsDokument.dokumentType() == DokumentType.PAYLOAD) {
        val ebxmlFormItem = PartData.FormItem(
            ebxml,
            {},
            HeadersBuilder().apply {
                this.append(MimeHeaders.CONTENT_ID, UUID.randomUUID().toString())
                this.append(MimeHeaders.CONTENT_TYPE, ContentType.Text.Xml.toString())
                this.append(MimeHeaders.CONTENT_TRANSFER_ENCODING, "base64")
            }.build()
        )
        val parts = mutableListOf<PartData>(ebxmlFormItem)
        ebmsDokument.attachments.first().let {
            PartData.FormItem(
                Base64.getMimeEncoder().encodeToString(it.bytes),
                {},
                HeadersBuilder().apply {
                    append(MimeHeaders.CONTENT_TRANSFER_ENCODING, "base64")
                    append(MimeHeaders.CONTENT_TYPE, it.contentType)
                    append(MimeHeaders.CONTENT_DISPOSITION, "attachment")
                    append(MimeHeaders.CONTENT_ID, it.contentId)
                }.build()
            ).also {
                parts.add(it)
            }
        }
        this.response.headers.append(MimeHeaders.CONTENT_TRANSFER_ENCODING, "8bit")
        this.respond(
            HttpStatusCode.OK,
            MultiPartFormDataContent(
                parts,
                "------=_Part" + System.currentTimeMillis() + "." + System.nanoTime(),
                ContentType.parse("""multipart/related;type="text/xml"""")
            )
        )
    } else {
        this.response.headers.append(MimeHeaders.CONTENT_TYPE, ContentType.Text.Xml.toString())
        this.response.headers.append(MimeHeaders.CONTENT_TRANSFER_ENCODING, "base64")
        this.respondText(status = HttpStatusCode.OK, text = ebxml)
    }
}
