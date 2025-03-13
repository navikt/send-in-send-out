package no.nav.emottak.ebms

import dev.reformator.stacktracedecoroutinator.runtime.DecoroutinatorRuntime
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.engine.embeddedServer
import io.ktor.server.metrics.micrometer.MicrometerMetrics
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.routing.routing
import io.micrometer.prometheus.PrometheusConfig
import io.micrometer.prometheus.PrometheusMeterRegistry
import kotlinx.serialization.json.Json
import no.nav.emottak.auth.AZURE_AD_AUTH
import no.nav.emottak.auth.AuthConfig
import no.nav.emottak.utils.isProdEnv
import no.nav.security.token.support.v3.tokenValidationSupport
import org.slf4j.LoggerFactory

internal val log = LoggerFactory.getLogger("no.nav.emottak.ebms.App")

fun main() {
    log.info("ebms-send-in starting")

    System.setProperty("io.ktor.http.content.multipart.skipTempFile", "true")
    embeddedServer(Netty, port = 8080, module = { ebmsSendInModule() })
        .also { it.engineConfig.maxChunkSize = 100000 }
        .start(wait = true)
}

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
        fagmeldingRoutes(appMicrometerRegistry)
        healthcheckRoutes(appMicrometerRegistry)
    }
}
