package no.nav.emottak.plugin

import arrow.core.Either
import arrow.core.NonEmptyList
import arrow.core.raise.either
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.micrometer.core.instrument.Timer.ResourceSample
import io.micrometer.prometheus.PrometheusMeterRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import no.nav.emottak.AZURE_AD_AUTH
import no.nav.emottak.PayloadRequestValidationError
import no.nav.emottak.log
import no.nav.emottak.repository.PayloadRepository
import no.nav.emottak.repository.PayloadRequest

private const val REFERENCE_ID = "referenceId"

fun Application.configureRoutes(registry: PrometheusMeterRegistry, db: PayloadRepository) {
    routing {
        registerHealthEndpoints(registry)
        authenticate(AZURE_AD_AUTH) {
            getPayloads(registry, db)
        }
    }
}

fun Route.registerHealthEndpoints(registry: PrometheusMeterRegistry) {
    get("/internal/health/liveness") {
        call.respondText("I'm alive! :)")
    }
    get("/internal/health/readiness") {
        call.respondText("I'm ready! :)")
    }
    get("/prometheus") {
        call.respond(registry.scrape())
    }
}

fun Route.getPayloads(registry: PrometheusMeterRegistry, db: PayloadRepository): Route = get("/payload/{$REFERENCE_ID}") {
    val request: Either<NonEmptyList<PayloadRequestValidationError>, PayloadRequest> = PayloadRequest(call.parameters[REFERENCE_ID])
    when (request) {
        is Either.Left -> { // Valideringsfeil:
            val msg = request.value.joinToString()
            log.warn("Validation failed:\n$msg")
            throw BadRequestException(msg)
        }
        is Either.Right -> { // OK request:
            val referenceId = request.value.referenceId
            val result = withContext(Dispatchers.IO) {
                timed(registry, "getPayloads") {
                    with(db) { either { retrieveWithoutContext(referenceId) } }
                }
            }
            when (result) {
                is Either.Right -> call.respond(HttpStatusCode.OK, result.value)
                is Either.Left -> {
                    log.warn("Did not find Payload.reference_id '$referenceId'")
                    call.respond(HttpStatusCode.NotFound, result.value)
                }
            }
        }
        else -> { // Teknisk feil:
            call.respond(HttpStatusCode.InternalServerError, request)
        }
    }
}

fun <T> timed(meterRegistry: PrometheusMeterRegistry, metricName: String, process: ResourceSample.() -> T): T =
    io.micrometer.core.instrument.Timer.resource(meterRegistry, metricName)
        .use {
            process(it)
        }
