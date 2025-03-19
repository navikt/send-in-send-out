package no.nav.emottak.ebms.route

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.raise.either
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.micrometer.prometheus.PrometheusMeterRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.slf4j.MDCContext
import kotlinx.coroutines.withContext
import no.nav.emottak.auth.AZURE_AD_AUTH
import no.nav.emottak.ebms.log
import no.nav.emottak.ebms.service.FagmeldingService
import no.nav.emottak.ebms.utils.receiveEither
import no.nav.emottak.melding.model.SendInRequest
import no.nav.emottak.melding.model.SendInResponse

fun Route.fagmeldingRoutes(prometheusMeterRegistry: PrometheusMeterRegistry) {
    authenticate(AZURE_AD_AUTH) {
        post("/fagmelding/synkron") {
            val sendInRequest = call.receiveEither<SendInRequest>().getOrElse { error ->
                log.error("SendInRequest mapping error", error)
                call.respond(HttpStatusCode.BadRequest, error.localizedMessage ?: "Mapping error")
                return@post
            }

            val mdcData = mapOf(
                "messageId" to sendInRequest.messageId,
                "conversationId" to sendInRequest.conversationId
            )

            withContext(Dispatchers.IO + MDCContext(mdcData)) {
                val result: Either<Throwable, SendInResponse> = either {
                    FagmeldingService.processRequest(sendInRequest, prometheusMeterRegistry).bind()
                }

                result.fold(
                    { error ->
                        log.error("Payload ${sendInRequest.payloadId} forwarding failed", error)
                        call.respond(
                            HttpStatusCode.BadRequest,
                            error.localizedMessage ?: error.javaClass.simpleName
                        )
                    },
                    { response ->
                        log.debug("Payload ${sendInRequest.payloadId} forwarding complete, returning response")
                        call.respond(response)
                    }
                )
            }
        }
    }
}
