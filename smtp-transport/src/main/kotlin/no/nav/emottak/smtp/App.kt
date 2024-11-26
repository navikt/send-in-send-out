package no.nav.emottak.smtp

import arrow.continuations.SuspendApp
import arrow.continuations.ktor.server
import arrow.core.raise.result
import arrow.fx.coroutines.resourceScope
import arrow.resilience.Schedule
import io.ktor.client.statement.HttpResponse
import io.ktor.server.netty.Netty
import io.ktor.utils.io.CancellationException
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import no.nav.emottak.config
import no.nav.emottak.metricsRegistry
import no.nav.emottak.plugin.configureContentNegotiation
import no.nav.emottak.plugin.configureMetrics
import no.nav.emottak.plugin.configureRoutes
import org.slf4j.LoggerFactory
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

internal val log = LoggerFactory.getLogger("no.nav.emottak.smtp")

fun main() = SuspendApp {
    val config = config()
    result {
        resourceScope {
            val registry = metricsRegistry()
            server(Netty, port = 8080, preWait = 5.seconds) {
                configureMetrics(registry)
                configureContentNegotiation()
                configureRoutes(registry)
            }
            val mailReaderService = MailReaderService(config)
            scheduleWithInitialDelay(mailReaderService::processMessages)

            awaitCancellation()
        }
    }
        .onFailure { error ->
            when (error) {
                is CancellationException -> {} // expected behaviour - normal shutdown
                else -> logError(error)
            }
        }
}

private suspend fun scheduleWithInitialDelay(block: suspend () -> Flow<Result<HttpResponse>>) {
    // Repeat every 5 minutes
    Schedule.spaced<Unit>(5.minutes)
        .delayed { attempt, _ ->
            // Delay by 1 minute only for the first attempt
            if (attempt == 0L) 1.minutes else 0.minutes
        }
        .repeat { block().collect {} }
}

private fun logError(t: Throwable) = log.error("Shutdown smtp-transport due to: ${t.stackTraceToString()}")
