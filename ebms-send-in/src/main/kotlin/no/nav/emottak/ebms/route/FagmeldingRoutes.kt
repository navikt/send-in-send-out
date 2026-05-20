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
import no.nav.emottak.legemelding.LegeMeldingService
import no.nav.emottak.log
import no.nav.emottak.sykmelding.SyfoMeldingService
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
    syfoMeldingService: SyfoMeldingService,
    legeMeldingService: LegeMeldingService,
    useAsyncIn: Boolean
) {
    authenticate(AZURE_AD_AUTH) {
        post("/fagmelding/synkron") {
            log.info("EbmsInPayload received synchronously, processing message")
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
                            syfoMeldingService,
                            legeMeldingService,
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
    syfoMeldingService: SyfoMeldingService,
    legeMeldingService: LegeMeldingService,
    call: RoutingCall
) {
    val result: Either<Throwable, Unit> = either {
        FagmeldingService.processRequestAsynchronously(
            sendInRequest,
            prometheusMeterRegistry,
            eventRegistrationService,
            trekkopplysningService,
            syfoMeldingService,
            legeMeldingService
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
            message = "Error when testing MQ connection for Trekkopplysning: " + e.localizedMessage ?: e.javaClass.simpleName
            log.error(message, e)
        }
        try {
            syfoMeldingService.verifyConnection()
            message = message + ", MQ connection for Sykmelding OK"
            log.info(message)
        } catch (e: Exception) {
            message = message + ", Error when testing MQ connection for Sykmelding: " + e.localizedMessage ?: e.javaClass.simpleName
            log.error(message, e)
        }
        try {
            legeMeldingService.verifyConnection()
            message = message + ", MQ connection for Legemelding OK"
            log.info(message)
        } catch (e: Exception) {
            message = message + ", Error when testing MQ connection for Legemelding: " + e.localizedMessage ?: e.javaClass.simpleName
            log.error(message, e)
        }
        call.respond(message)
    }
}

fun Route.sendTestSykmelding(
    syfoMeldingService: SyfoMeldingService
) {
    get("/testSykmelding") {
        log.info("Sending Sykmelding test message......")
        try {
            val fellesformatXml = this::class.java.classLoader.getResourceAsStream("tmp_testsykemelding.xml")!!.readAllBytes().decodeToString()
            log.info("Will send Sykmelding: " + fellesformatXml)
            syfoMeldingService.sendMessage(fellesformatXml)
            log.info("Sykmelding sent")
            call.respond("Sykmelding sent")
        } catch (e: Exception) {
            log.error("Error sending Sykmelding", e)
            call.respond(e.localizedMessage ?: e.javaClass.simpleName)
        }
    }
}
