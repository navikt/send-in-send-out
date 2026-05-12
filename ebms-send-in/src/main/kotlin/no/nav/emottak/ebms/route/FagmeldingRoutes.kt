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
import io.micrometer.prometheus.PrometheusMeterRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.slf4j.MDCContext
import kotlinx.coroutines.withContext
import no.nav.emottak.auth.AZURE_AD_AUTH
import no.nav.emottak.ebms.service.FagmeldingService
import no.nav.emottak.ebms.utils.SupportedAsyncServiceType
import no.nav.emottak.ebms.utils.SupportedAsyncServiceType.Companion.toSupportedAsyncService
import no.nav.emottak.ebms.utils.receiveEither
import no.nav.emottak.log
import no.nav.emottak.trekkopplysning.TrekkopplysningService
import no.nav.emottak.util.EventRegistrationService
import no.nav.emottak.utils.common.model.SendInRequest
import no.nav.emottak.utils.common.model.SendInResponse
import no.nav.emottak.utils.common.parseOrGenerateUuid
import no.nav.emottak.utils.kafka.model.EventType
import no.nav.emottak.utils.serialization.toEventDataJson

fun Route.fagmeldingRoutes(
    prometheusMeterRegistry: PrometheusMeterRegistry,
    eventRegistrationService: EventRegistrationService,
    trekkopplysningService: TrekkopplysningService,
    useAsyncIn: Boolean
) {
    authenticate(AZURE_AD_AUTH) {
        post("/fagmelding/synkron") {
            log.debug("EbmsInPayload received synchronously, processing message")
            val sendInRequest = call.receiveEither<SendInRequest>().getOrElse { error ->
                log.error("SendInRequest mapping error", error)
                call.respond(HttpStatusCode.BadRequest, error.localizedMessage ?: "Mapping error")
                return@post
            }

            val mdcData = mapOf(
                "messageId" to sendInRequest.messageId,
                "conversationId" to sendInRequest.conversationId,
                "cpaId" to sendInRequest.cpaId,
                "requestId" to sendInRequest.requestId
            )

            withContext(Dispatchers.IO + MDCContext(mdcData)) {
                // midlertidig hack til vi har async kall fra ebmxl-prosessor
                if (useAsyncIn) {
                    if (sendInRequest.addressing.service.toSupportedAsyncService() == SupportedAsyncServiceType.Trekkopplysning) {
                        log.warn("Trekkopplysning is received synchronously, and will be further processed asynchronously. However the synchronous response will be empty and probably regarded as an error.")
                        callTrekkopplysningAsync(
                            sendInRequest,
                            prometheusMeterRegistry,
                            eventRegistrationService,
                            trekkopplysningService,
                            call
                        )
                        return@withContext
                    }
                }
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
                        eventRegistrationService.registerEvent(
                            EventType.ERROR_WHILE_SENDING_MESSAGE_TO_FAGSYSTEM,
                            requestId = sendInRequest.requestId.parseOrGenerateUuid(),
                            messageId = sendInRequest.messageId,
                            eventData = Exception(error).toEventDataJson(),
                            conversationId = sendInRequest.conversationId
                        )
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
    }
}

private suspend fun callTrekkopplysningAsync(
    sendInRequest: SendInRequest,
    prometheusMeterRegistry: PrometheusMeterRegistry,
    eventRegistrationService: EventRegistrationService,
    trekkopplysningService: TrekkopplysningService,
    call: RoutingCall
) {
    val result: Either<Throwable, Unit> = either {
        FagmeldingService.processRequestAsynchronously(
            sendInRequest,
            prometheusMeterRegistry,
            eventRegistrationService,
            trekkopplysningService
        ).bind()
    }

    result.fold(
        { error ->
            log.error("Payload ${sendInRequest.payloadId} forwarding failed", error)
            eventRegistrationService.registerEvent(
                EventType.ERROR_WHILE_SENDING_MESSAGE_TO_FAGSYSTEM,
                requestId = sendInRequest.requestId.parseOrGenerateUuid(),
                messageId = sendInRequest.messageId,
                eventData = Exception(error).toEventDataJson(),
                conversationId = sendInRequest.conversationId
            )
            call.respond(
                HttpStatusCode.BadRequest,
                error.localizedMessage ?: error.javaClass.simpleName
            )
        },
        {
            log.info("Trekkopplysning ${sendInRequest.payloadId} forwarding complete, no response")
            call.respond("Trekkopplysning forwarding complete")
        }
    )
}

fun Route.verifyMq(
    trekkopplysningService: TrekkopplysningService
) {
    get("/testMq") {
        log.info("Testing MQ......")
        try {
            trekkopplysningService.verifyConnection()
            log.info("MQ connection OK")
            call.respond("MQ connection OK")
        } catch (e: Exception) {
            log.error("Error testing MQ", e)
            call.respond(e.localizedMessage ?: e.javaClass.simpleName)
        }
    }
}
