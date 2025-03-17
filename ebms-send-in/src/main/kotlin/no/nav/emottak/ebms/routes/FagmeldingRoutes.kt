package no.nav.emottak.ebms.routes

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
import kotlinx.coroutines.withContext
import no.nav.emottak.auth.AZURE_AD_AUTH
import no.nav.emottak.ebms.log
import no.nav.emottak.ebms.service.FagmeldingService
import no.nav.emottak.ebms.utils.receiveEither
import no.nav.emottak.melding.model.SendInRequest
import no.nav.emottak.melding.model.SendInResponse
import no.nav.emottak.util.marker

fun Route.fagmeldingRoutes(prometheusMeterRegistry: PrometheusMeterRegistry) {
    authenticate(AZURE_AD_AUTH) {
        post("/fagmelding/synkron") {
            val sendInRequest = call.receiveEither<SendInRequest>().getOrElse { error ->
                log.error("SendInRequest mapping error", error)
                call.respond(HttpStatusCode.BadRequest, error.localizedMessage ?: "Mapping error")
                return@post
            }

            log.info(sendInRequest.marker(), "Payload ${sendInRequest.payloadId} is forwarded to backend service")

            val result: Either<Throwable, SendInResponse> = either {
                withContext(Dispatchers.IO) {
                    FagmeldingService.processRequest(sendInRequest, prometheusMeterRegistry).bind()
                }
            }

            result.fold(
                { error ->
                    log.error(sendInRequest.marker(), "Payload ${sendInRequest.payloadId} forwarding failed", error)
                    call.respond(
                        HttpStatusCode.BadRequest,
                        error.localizedMessage ?: error.javaClass.simpleName
                    )
                },
                { response ->
                    log.debug(
                        sendInRequest.marker(),
                        "Payload ${sendInRequest.payloadId} forwarding complete, returning response"
                    )
                    call.respond(response)
                }
            )
        }
    }
}
