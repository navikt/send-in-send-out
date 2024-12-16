package no.nav.emottak.plugin

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.NotFoundException
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.micrometer.core.instrument.Timer.ResourceSample
import io.micrometer.prometheus.PrometheusMeterRegistry
import no.nav.emottak.AZURE_AD_AUTH
import no.nav.emottak.log
import no.nav.emottak.repository.PayloadRepository

private const val REFERENCE_ID = "referenceId"

fun Application.configureRoutes(registry: PrometheusMeterRegistry, db: PayloadRepository) {
    routing {
        get("/internal/health/liveness") {
            call.respondText("I'm alive! :)")
        }
        get("/internal/health/readiness") {
            call.respondText("I'm ready! :)")
        }
        get("/prometheus") {
            call.respond(registry.scrape())
        }
        authenticate(AZURE_AD_AUTH) {
            getPayload(registry, db)
        }
    }
}

fun Route.getPayload(registry: PrometheusMeterRegistry, db: PayloadRepository): Route = get("/payload/{$REFERENCE_ID}") {
    val referenceId = call.parameters[REFERENCE_ID] ?: throw BadRequestException("Mangler $REFERENCE_ID")
    runCatching {
        timed(registry, "getPayload") {
            db.getPayload(referenceId)
        }
    }.onSuccess {
        call.respond(HttpStatusCode.OK, it)
    }.onFailure {
        log.warn("Feil ved henting av Payload.reference_id \"$referenceId\"", it)
        if (it is NotFoundException) {
            call.respond(HttpStatusCode.NotFound, "Reference-id not found")
        } else {
            call.respond(HttpStatusCode.InternalServerError, it)
        }
    }
}

fun <T> timed(meterRegistry: PrometheusMeterRegistry, metricName: String, process: ResourceSample.() -> T): T =
    io.micrometer.core.instrument.Timer.resource(meterRegistry, metricName)
        .use {
            process(it)
        }
