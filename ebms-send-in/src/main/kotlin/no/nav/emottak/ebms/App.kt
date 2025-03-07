package no.nav.emottak.ebms

import dev.reformator.stacktracedecoroutinator.runtime.DecoroutinatorRuntime
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.authenticate
import io.ktor.server.engine.embeddedServer
import io.ktor.server.metrics.micrometer.MicrometerMetrics
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import io.micrometer.core.instrument.Timer.ResourceSample
import io.micrometer.prometheus.PrometheusConfig
import io.micrometer.prometheus.PrometheusMeterRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import no.nav.emottak.auth.AZURE_AD_AUTH
import no.nav.emottak.auth.AuthConfig
import no.nav.emottak.ebms.SupportedService.Companion.toSupportedService
import no.nav.emottak.fellesformat.FellesFormatXmlMarshaller
import no.nav.emottak.fellesformat.asEIFellesFormat
import no.nav.emottak.frikort.frikortsporring
import no.nav.emottak.frikort.frikortsporringMengde
import no.nav.emottak.melding.model.SendInRequest
import no.nav.emottak.melding.model.SendInResponse
import no.nav.emottak.pasientliste.PasientlisteService
import no.nav.emottak.utbetaling.UtbetalingClient
import no.nav.emottak.utbetaling.UtbetalingXmlMarshaller
import no.nav.emottak.util.LogLevel
import no.nav.emottak.util.asJson
import no.nav.emottak.util.asXml
import no.nav.emottak.util.marker
import no.nav.emottak.utils.isProdEnv
import no.nav.security.token.support.v3.tokenValidationSupport
import org.slf4j.LoggerFactory
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

internal val log = LoggerFactory.getLogger("no.nav.emottak.ebms.App")

fun main() {
    log.info("ebms-send-in starting")

    System.setProperty("io.ktor.http.content.multipart.skipTempFile", "true")
    embeddedServer(Netty, port = 8080, module = Application::ebmsSendInModule)
        .also { it.engineConfig.maxChunkSize = 100000 }
        .start(wait = true)
}

fun <T> timed(meterRegistry: PrometheusMeterRegistry, metricName: String, process: ResourceSample.() -> T): T =
    io.micrometer.core.instrument.Timer.resource(meterRegistry, metricName)
        .use {
            process(it)
        }

@OptIn(ExperimentalUuidApi::class)
fun Application.ebmsSendInModule() {
    install(ContentNegotiation) {
        json(
            Json {
                explicitNulls = false
                ignoreUnknownKeys = true
            }
        )
    }
    val appMicrometerRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
    install(MicrometerMetrics) {
        registry = appMicrometerRegistry
    }

    install(Authentication) {
        tokenValidationSupport(AZURE_AD_AUTH, AuthConfig.getTokenSupportConfig())
    }

    if (!isProdEnv()) {
        DecoroutinatorRuntime.load()
    }

    routing {
        authenticate(AZURE_AD_AUTH) {
            post("/fagmelding/synkron") {
                val sendInRequest = try {
                    this.call.receive(SendInRequest::class)
                } catch (e: Exception) {
                    log.error("SendInRequest mapping error", e)
                    throw e
                }
                log.info(sendInRequest.marker(), "Payload ${sendInRequest.payloadId} videresendes til fagsystem")
                runCatching {
                    withContext(Dispatchers.IO) {
                        when (sendInRequest.addressing.service.toSupportedService()) {
                            SupportedService.Inntektsforesporsel ->
                                timed(appMicrometerRegistry, "Inntektsforesporsel") {
                                    UtbetalingClient.behandleInntektsforesporsel(
                                        sendInRequest.messageId,
                                        sendInRequest.conversationId,
                                        sendInRequest.payload
                                    ).let { msgHeadResponse ->
                                        SendInResponse(
                                            messageId = sendInRequest.messageId,
                                            conversationId = sendInRequest.conversationId,
                                            addressing = sendInRequest.addressing.replyTo(
                                                sendInRequest.addressing.service,
                                                msgHeadResponse.msgInfo.type.v
                                            ),
                                            payload = UtbetalingXmlMarshaller.marshalToByteArray(msgHeadResponse),
                                            requestId = Uuid.random().toString()
                                        )
                                    }
                                }

                            SupportedService.HarBorgerEgenandelFritak, SupportedService.HarBorgerFrikort ->
                                timed(appMicrometerRegistry, "frikort-sporing") {
                                    with(sendInRequest.asEIFellesFormat()) {
                                        frikortsporring(this).let { frikortsporringResponse ->
                                            SendInResponse(
                                                messageId = sendInRequest.messageId,
                                                conversationId = sendInRequest.conversationId,
                                                addressing = sendInRequest.addressing.replyTo(
                                                    frikortsporringResponse.eiFellesformat.mottakenhetBlokk.ebService,
                                                    frikortsporringResponse.eiFellesformat.mottakenhetBlokk.ebAction
                                                ),
                                                payload = FellesFormatXmlMarshaller.marshalToByteArray(frikortsporringResponse.eiFellesformat.msgHead),
                                                requestId = Uuid.random().toString()
                                            )
                                        }
                                    }
                                }

                            SupportedService.HarBorgerFrikortMengde ->
                                timed(appMicrometerRegistry, "frikortMengde-sporing") {
                                    with(sendInRequest.asEIFellesFormat()) {
                                        frikortsporringMengde(this).let { frikortsporringMengdeResponse ->
                                            SendInResponse(
                                                messageId = sendInRequest.messageId,
                                                conversationId = sendInRequest.conversationId,
                                                addressing = sendInRequest.addressing.replyTo(
                                                    frikortsporringMengdeResponse.eiFellesformat.mottakenhetBlokk.ebService,
                                                    frikortsporringMengdeResponse.eiFellesformat.mottakenhetBlokk.ebAction
                                                ),
                                                payload = FellesFormatXmlMarshaller.marshalToByteArray(frikortsporringMengdeResponse.eiFellesformat.msgHead),
                                                requestId = Uuid.random().toString()
                                            )
                                        }
                                    }
                                }

                            SupportedService.PasientlisteForesporsel ->
                                timed(appMicrometerRegistry, "PasientlisteForesporsel") {
                                    if (isProdEnv()) {
                                        throw NotImplementedError("PasientlisteForesporsel is used in prod. Feature is not ready. Aborting.")
                                    }
                                    with(sendInRequest.asEIFellesFormat()) {
                                        log.asXml(LogLevel.DEBUG, "Wrapped message (fellesformatRequest)", this, sendInRequest.marker())
                                        PasientlisteService.pasientlisteForesporsel(this).let { fellesformatResponse ->
                                            SendInResponse(
                                                messageId = sendInRequest.messageId,
                                                conversationId = sendInRequest.conversationId,
                                                addressing = sendInRequest.addressing.replyTo(
                                                    fellesformatResponse.mottakenhetBlokk.ebService,
                                                    fellesformatResponse.mottakenhetBlokk.ebAction
                                                ),
                                                payload = FellesFormatXmlMarshaller.marshalToByteArray(fellesformatResponse.appRec),
                                                requestId = Uuid.random().toString()
                                            ).also {
                                                log.asJson(LogLevel.DEBUG, "Sending SendInResponse", it, SendInResponse.serializer(), sendInRequest.marker())
                                            }
                                        }
                                    }
                                }

                            SupportedService.Unsupported -> {
                                throw NotImplementedError("Service: ${sendInRequest.addressing.service} is not implemented")
                            }
                        }
                    }
                }.onSuccess {
                    log.debug(
                        sendInRequest.marker(),
                        "Payload ${sendInRequest.payloadId} videresending til fagsystem ferdig, svar mottatt og returnert"
                    )
                    call.respond(it)
                }.onFailure {
                    log.error(sendInRequest.marker(), "Payload ${sendInRequest.payloadId} videresending feilet", it)
                    call.respond(HttpStatusCode.BadRequest, it.localizedMessage ?: it.cause?.message ?: it.javaClass.simpleName)
                }
            }
        }

        registerHealthEndpoints(appMicrometerRegistry)
    }
}
