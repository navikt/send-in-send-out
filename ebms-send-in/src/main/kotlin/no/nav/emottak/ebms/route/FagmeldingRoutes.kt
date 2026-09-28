package no.nav.emottak.ebms.route

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.raise.either
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.RoutingCall
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.slf4j.MDCContext
import kotlinx.coroutines.withContext
import no.nav.emottak.auth.AZURE_AD_AUTH
import no.nav.emottak.ebms.service.FagmeldingService
import no.nav.emottak.ebms.utils.receiveEither
import no.nav.emottak.legemelding.LegeMeldingService
import no.nav.emottak.log
import no.nav.emottak.sykmelding.SyfoMeldingService
import no.nav.emottak.trekkopplysning.TrekkopplysningService
import no.nav.emottak.util.EventRegistrationService
import no.nav.emottak.utils.common.model.SendInRequest
import no.nav.emottak.utils.common.model.SendInResponse

fun Route.fagmeldingRoutes(
    prometheusMeterRegistry: PrometheusMeterRegistry,
    eventRegistrationService: EventRegistrationService,
    trekkopplysningService: TrekkopplysningService,
    syfoMeldingService: SyfoMeldingService,
    legeMeldingService: LegeMeldingService
) {
    authenticate(AZURE_AD_AUTH) {
        post("/fagmelding/synkron") {
            log.debug("EbmsInPayload received synchronously, processing message")
            val sendInRequest = call.receiveSendInRequestOrRespondError() ?: return@post

            withContext(Dispatchers.IO + MDCContext(sendInRequest.mdcData())) {
                val result: Either<Throwable, SendInResponse> = either {
                    FagmeldingService.processRequestSynchronously(
                        sendInRequest,
                        prometheusMeterRegistry,
                        eventRegistrationService
                    ).bind()
                }

                result.fold(
                    { error ->
                        log.error("Payload ${sendInRequest.payloadId} sync processing failed", error)
                        eventRegistrationService.registerErrorOnHandoverToFagsystem(sendInRequest, error)
                        call.respond(
                            HttpStatusCode.BadRequest,
                            error.localizedMessage ?: error.javaClass.simpleName
                        )
                    },
                    { response ->
                        log.info("Payload ${sendInRequest.payloadId} sync processing complete, returning response")
                        call.respond(response)
                    }
                )
            }
        }

        post("/fagmelding/asynkron") {
            log.debug("EbmsInPayload received asynchronously, processing message")
            val sendInRequest = call.receiveSendInRequestOrRespondError() ?: return@post

            withContext(Dispatchers.IO + MDCContext(sendInRequest.mdcData())) {
                FagmeldingService.processRequestAsynchronously(
                    sendInRequest,
                    prometheusMeterRegistry,
                    eventRegistrationService,
                    trekkopplysningService,
                    syfoMeldingService,
                    legeMeldingService
                ).fold(
                    { error ->
                        log.error("EbmsInPayload ${sendInRequest.payloadId} async forwarding failed", error)
                        eventRegistrationService.registerErrorOnHandoverToFagsystem(sendInRequest, error)
                        call.respond(
                            HttpStatusCode.BadRequest,
                            error.localizedMessage ?: error.javaClass.simpleName
                        )
                    },
                    {
                        log.info("EbmsInPayload ${sendInRequest.payloadId} async forwarding complete")
                        call.respond(HttpStatusCode.Accepted)
                    }
                )
            }
        }
    }
}

private suspend fun RoutingCall.receiveSendInRequestOrRespondError(): SendInRequest? {
    return this.receiveEither<SendInRequest>().getOrElse { error ->
        log.error("SendInRequest mapping error", error)
        this.respond(HttpStatusCode.BadRequest, error.localizedMessage ?: "Mapping error")
        null
    }
}

internal fun SendInRequest.mdcData() =
    mapOf(
        "messageId" to messageId,
        "conversationId" to conversationId,
        "cpaId" to cpaId,
        "requestId" to requestId,
        "service" to addressing.service,
        "action" to addressing.action
    )

fun Route.verifyMq(
    trekkopplysningService: TrekkopplysningService,
    syfoMeldingService: SyfoMeldingService,
    legeMeldingService: LegeMeldingService
) {
    get("/testMq") {
        log.info("Testing MQ......")
        var message = ""
        try {
            trekkopplysningService.verifyConnection()
            message = "MQ connection for Trekkopplysning OK"
            log.info(message)
        } catch (e: Exception) {
            message = "Error when testing MQ connection for Trekkopplysning: " + (e.localizedMessage ?: e.javaClass.simpleName)
            log.error(message, e)
        }
        try {
            syfoMeldingService.verifyConnection()
            message += ", MQ connection for Sykmelding OK"
            log.info(message)
        } catch (e: Exception) {
            message = message + ", Error when testing MQ connection for Sykmelding: " + (e.localizedMessage ?: e.javaClass.simpleName)
            log.error(message, e)
        }
        try {
            legeMeldingService.verifyConnection()
            message += ", MQ connection for Legemelding OK"
            log.info(message)
        } catch (e: Exception) {
            message = message + ", Error when testing MQ connection for Legemelding: " + (e.localizedMessage ?: e.javaClass.simpleName)
            log.error(message, e)
        }
        call.respond(message)
    }
}
