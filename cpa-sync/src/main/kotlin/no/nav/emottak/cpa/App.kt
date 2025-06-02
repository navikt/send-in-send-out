package no.nav.emottak.cpa

import dev.reformator.stacktracedecoroutinator.runtime.DecoroutinatorRuntime
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.metrics.micrometer.MicrometerMetrics
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.routing.routing
import io.micrometer.prometheus.PrometheusConfig
import io.micrometer.prometheus.PrometheusMeterRegistry
import no.nav.emottak.utils.environment.isProdEnv
import org.slf4j.LoggerFactory

fun main() {
    // if (getEnvVar("NAIS_CLUSTER_NAME", "local") != "prod-fss") {
    DecoroutinatorRuntime.load()
    // }
    embeddedServer(Netty, port = 8080, module = Application::myApplicationModule).start(wait = true)
}

internal val log = LoggerFactory.getLogger("no.nav.emottak.cpa")

fun Application.myApplicationModule() {
    install(ContentNegotiation) {
        json()
    }
    val appMicrometerRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
    install(MicrometerMetrics) {
        registry = appMicrometerRegistry
    }
    routing {
        if (!isProdEnv()) {
            testAzureAuthToCpaRepo()
        }
        registerHealthEndpoints(appMicrometerRegistry)
        cpaSync()
    }
}
