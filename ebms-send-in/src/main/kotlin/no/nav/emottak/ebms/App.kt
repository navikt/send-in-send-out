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
import no.nav.emottak.fellesformat.FellesFormatXmlMarshaller
import no.nav.emottak.fellesformat.wrapMessageInEIFellesFormat
import no.nav.emottak.frikort.frikortsporring
import no.nav.emottak.frikort.frikortsporringMengde
import no.nav.emottak.melding.model.SendInRequest
import no.nav.emottak.melding.model.SendInResponse
import no.nav.emottak.pasientliste.PasientlisteService
import no.nav.emottak.utbetaling.UtbetalingClient
import no.nav.emottak.utbetaling.UtbetalingXmlMarshaller
import no.nav.emottak.util.marker
import no.nav.emottak.utils.isProdEnv
import no.nav.security.token.support.v3.tokenValidationSupport
import org.slf4j.LoggerFactory
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

internal val log = LoggerFactory.getLogger("no.nav.emottak.ebms.App")

fun main() {
    log.info("ebms-send-in starting")
    // val database = Database(mapHikariConfig(DatabaseConfig()))
    // database.migrate()

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
                val request = try {
                    this.call.receive(SendInRequest::class)
                } catch (e: Exception) {
                    log.error("SendInRequest mapping error", e)
                    throw e
                }
                runCatching {
                    log.info(request.marker(), "Payload ${request.payloadId} videresendes til fagsystem")
                    // TODO: Logg til Event-db? Med request.requestId
                    val responseRequestId = Uuid.random().toString()
                    withContext(Dispatchers.IO) {
                        when (request.addressing.service) {
                            "Inntektsforesporsel" ->
                                timed(appMicrometerRegistry, "Inntektsforesporsel") {
                                    UtbetalingClient.behandleInntektsforesporsel(request).let {
                                        SendInResponse(
                                            request.messageId,
                                            request.conversationId,
                                            request.addressing.replyTo(request.addressing.service, it.msgInfo.type.v),
                                            UtbetalingXmlMarshaller.marshalToByteArray(it),
                                            responseRequestId
                                        )
                                    }
                                }

                            "HarBorgerEgenandelFritak", "HarBorgerFrikort" -> timed(
                                appMicrometerRegistry,
                                "frikort-sporing"
                            ) {
                                frikortsporring(wrapMessageInEIFellesFormat(request)).let {
                                    SendInResponse(
                                        request.messageId,
                                        request.conversationId,
                                        request.addressing.replyTo(
                                            it.eiFellesformat.mottakenhetBlokk.ebService,
                                            it.eiFellesformat.mottakenhetBlokk.ebAction
                                        ),
                                        FellesFormatXmlMarshaller.marshalToByteArray(it.eiFellesformat.msgHead),
                                        responseRequestId
                                    )
                                }
                            }

                            "HarBorgerFrikortMengde" -> timed(
                                appMicrometerRegistry,
                                "frikortMengde-sporing"
                            ) {
                                frikortsporringMengde(wrapMessageInEIFellesFormat(request)).let {
                                    SendInResponse(
                                        request.messageId,
                                        request.conversationId,
                                        request.addressing.replyTo(
                                            it.eiFellesformat.mottakenhetBlokk.ebService,
                                            it.eiFellesformat.mottakenhetBlokk.ebAction
                                        ),
                                        FellesFormatXmlMarshaller.marshalToByteArray(it.eiFellesformat.msgHead),
                                        responseRequestId
                                    )
                                }
                            }

                            "PasientlisteForesporsel" -> timed(appMicrometerRegistry, "PasientlisteForesporsel") {
                                if (isProdEnv()) {
                                    throw NotImplementedError("PasientlisteForesporsel is used in prod. Feature is not ready. Aborting.")
                                }
                                PasientlisteService.pasientlisteForesporsel(request, responseRequestId)
                            }

                            else -> {
                                throw NotImplementedError("Service: ${request.addressing.service} is not implemented")
                            }
                        }
                    }
                }.onSuccess {
                    log.trace(
                        request.marker(),
                        "Payload ${request.payloadId} videresending til fagsystem ferdig, svar mottatt og returnert"
                    )
                    // TODO: Event-logging OK
                    call.respond(it)
                }.onFailure {
                    log.error(request.marker(), "Payload ${request.payloadId} videresending feilet", it)
                    // TODO: Event-logging Feil
                    // TODO: Hvordan generere og returnere requestId ved feil? Det trengs ved Event-logging. Med unntak av NotImplementedError?
                    call.respond(HttpStatusCode.BadRequest, it.localizedMessage ?: it.cause?.message ?: it.javaClass.simpleName)
                }
            }
        }

        registerHealthEndpoints(appMicrometerRegistry)
    }
}
